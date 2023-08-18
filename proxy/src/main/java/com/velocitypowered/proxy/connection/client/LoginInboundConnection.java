/*
 * Copyright (C) 2021-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

/**
 * Handles the actual login stage of a player logging in.
 */
public class LoginInboundConnection implements LoginPhaseConnection, KeyIdentifiable {

  private static final AtomicIntegerFieldUpdater<LoginInboundConnection> SEQUENCE_UPDATER =
      AtomicIntegerFieldUpdater.newUpdater(LoginInboundConnection.class, "sequenceCounter");

  private final InitialInboundConnection delegate;
  private final Int2ObjectMap<MessageConsumer> outstandingResponses;
  private volatile int sequenceCounter;
  private final Queue<LoginPluginMessage> loginMessagesToSend;
  private volatile Runnable onAllMessagesHandled;
  private volatile boolean loginEventFired;
  private @MonotonicNonNull IdentifiedKey playerKey;

  LoginInboundConnection(
      InitialInboundConnection delegate) {
    this.delegate = delegate;
    this.outstandingResponses = Int2ObjectSyncMap.hashmap();
    this.loginMessagesToSend = new ArrayDeque<>();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return delegate.getRemoteAddress();
  }

  @Override
  public Optional<InetSocketAddress> getVirtualHost() {
    return delegate.getVirtualHost();
  }

  @Override
  public boolean isActive() {
    return delegate.isActive();
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return delegate.getProtocolVersion();
  }

  @Override
  public void sendLoginPluginMessage(ChannelIdentifier identifier, byte[] contents,
      MessageConsumer consumer) {
    if (identifier == null) {
      throw new NullPointerException("identifier");
    }
    if (contents == null) {
      throw new NullPointerException("contents");
    }
    if (consumer == null) {
      throw new NullPointerException("consumer");
    }
    if (delegate.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      throw new IllegalStateException("Login plugin messages can only be sent to clients running "
          + "Minecraft 1.13 and above");
    }

    final int id = SEQUENCE_UPDATER.incrementAndGet(this);
    this.outstandingResponses.put(id, consumer);

    final LoginPluginMessage message = new LoginPluginMessage(id, identifier.getId(),
        Unpooled.wrappedBuffer(contents));
    if (!this.loginEventFired) {
      this.loginMessagesToSend.add(message);
    } else {
      this.delegate.getConnection().write(message);
    }
  }

  /**
   * Disconnects the connection from the server.
   *
   * @param reason the reason for disconnecting
   */
  public void disconnect(Component reason) {
    this.delegate.disconnect(reason);
    this.cleanup();
  }

  void cleanup() {
    this.loginMessagesToSend.clear();
    this.outstandingResponses.clear();
    this.onAllMessagesHandled = null;
  }

  void handleLoginPluginResponse(final LoginPluginResponse response) {
    final MessageConsumer consumer = this.outstandingResponses.remove(response.getId());
    if (consumer != null) {
      try {
        consumer.onMessageResponse(response.isSuccess() ? ByteBufUtil.getBytes(response.content())
            : null);
      } finally {
        final Runnable onAllMessagesHandled = this.onAllMessagesHandled;
        if (this.outstandingResponses.isEmpty() && onAllMessagesHandled != null) {
          onAllMessagesHandled.run();
        }
      }
    }
  }

  void loginEventFired(final Runnable onAllMessagesHandled) {
    this.loginEventFired = true;
    this.onAllMessagesHandled = onAllMessagesHandled;
    if (!this.loginMessagesToSend.isEmpty()) {
      LoginPluginMessage message;
      while ((message = this.loginMessagesToSend.poll()) != null) {
        this.delegate.getConnection().delayedWrite(message);
      }
      this.delegate.getConnection().flush();
    } else {
      onAllMessagesHandled.run();
    }
  }

  MinecraftConnection delegatedConnection() {
    return delegate.getConnection();
  }

  public void setPlayerKey(IdentifiedKey playerKey) {
    this.playerKey = playerKey;
  }

  @Override
  public IdentifiedKey getIdentifiedKey() {
    return playerKey;
  }
}
