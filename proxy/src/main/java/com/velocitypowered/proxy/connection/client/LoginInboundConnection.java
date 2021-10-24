package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.LoginPhaseConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.net.InetSocketAddress;
import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import space.vectrix.flare.fastutil.Int2ObjectSyncMap;

public class LoginInboundConnection implements LoginPhaseConnection {

  private final InitialInboundConnection delegate;
  private final Int2ObjectMap<MessageConsumer> outstandingResponses;
  private final AtomicInteger sequenceCounter;
  private final AtomicInteger outstandingMessages;
  private final Queue<LoginPluginMessage> loginMessagesToSend;
  private volatile Runnable onAllMessagesHandled;
  private volatile boolean loginEventFired;

  public LoginInboundConnection(
      InitialInboundConnection delegate) {
    this.delegate = delegate;
    this.outstandingResponses = Int2ObjectSyncMap.hashmap();
    this.sequenceCounter = new AtomicInteger();
    this.outstandingMessages = new AtomicInteger();
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

    final int id = this.sequenceCounter.getAndIncrement();
    this.outstandingResponses.put(id, consumer);
    this.outstandingMessages.incrementAndGet();

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
   * @param reason the reason for disconnecting
   */
  public void disconnect(Component reason) {
    this.delegate.disconnect(reason);
    this.cleanup();
  }

  public void cleanup() {
    this.loginMessagesToSend.clear();
    this.outstandingResponses.clear();
    this.onAllMessagesHandled = null;
  }

  public void handleLoginPluginResponse(final LoginPluginResponse response) {
    final MessageConsumer consumer = this.outstandingResponses.remove(response.getId());
    if (consumer != null) {
      try {
        consumer.onMessageResponse(response.isSuccess() ? ByteBufUtil.getBytes(response.content())
            : null);
      } finally {
        final Runnable onAllMessagesHandled = this.onAllMessagesHandled;
        if (this.outstandingMessages.decrementAndGet() == 0 && onAllMessagesHandled != null) {
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
}
