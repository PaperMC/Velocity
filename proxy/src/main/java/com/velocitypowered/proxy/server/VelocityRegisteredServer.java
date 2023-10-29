/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.server;

import static com.velocitypowered.proxy.network.Connections.HANDLER;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.pipeline.initializers.BackendChannelInitializer;
import com.velocitypowered.proxy.network.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a server registered on the proxy.
 */
public class VelocityRegisteredServer implements RegisteredServer, ForwardingAudience {

  private final @Nullable VelocityServer server;
  private final ServerInfo serverInfo;
  private final Map<UUID, ConnectedPlayer> players = new ConcurrentHashMap<>();

  public VelocityRegisteredServer(@Nullable VelocityServer server, ServerInfo serverInfo) {
    this.server = server;
    this.serverInfo = Preconditions.checkNotNull(serverInfo, "serverInfo");
  }

  @Override
  public ServerInfo serverInfo() {
    return serverInfo;
  }

  @Override
  public Collection<Player> players() {
    return ImmutableList.copyOf(players.values());
  }

  @Override
  public CompletableFuture<ServerPing> ping(PingOptions pingOptions) {
    return ping(null, pingOptions);
  }

  @Override
  public CompletableFuture<ServerPing> ping() {
    return ping(null, PingOptions.DEFAULT);
  }

  /**
   * Pings the specified server using the specified event {@code loop}, claiming to be {@code
   * version}.
   *
   * @param loop    the event loop to use
   * @param pingOptions the options to apply to this ping
   * @return the server list ping response
   */
  public CompletableFuture<ServerPing> ping(@Nullable EventLoop loop, PingOptions pingOptions) {
    if (server == null) {
      throw new IllegalStateException("No Velocity proxy instance available");
    }
    CompletableFuture<ServerPing> pingFuture = new CompletableFuture<>();
    long timeoutMs = pingOptions.timeout() == 0
        ? server.configuration().getReadTimeout() : pingOptions.timeout();
    server.createBootstrap(loop, serverInfo.address())
        .handler(new BackendChannelInitializer(timeoutMs))
        .connect(serverInfo.address())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            Channel ch = future.channel();
            MinecraftConnection conn = new MinecraftConnection(ch, server);
            ch.pipeline().addLast(HANDLER, conn);

            PingSessionHandler handler = new PingSessionHandler(pingFuture,
                VelocityRegisteredServer.this, conn, pingOptions.protocolVersion());
            conn.setActiveSessionHandler(StateRegistry.HANDSHAKE, handler);
          } else {
            pingFuture.completeExceptionally(future.cause());
          }
        });
    return pingFuture;
  }

  public void addPlayer(ConnectedPlayer player) {
    players.put(player.uuid(), player);
  }

  public void removePlayer(ConnectedPlayer player) {
    players.remove(player.uuid(), player);
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    return sendPluginMessage(identifier, Unpooled.wrappedBuffer(data));
  }

  /**
   * Sends a plugin message to the server through this connection. The message will be released
   * afterwards.
   *
   * @param identifier the channel ID to use
   * @param data       the data
   * @return whether or not the message was sent
   */
  public boolean sendPluginMessage(ChannelIdentifier identifier, ByteBuf data) {
    for (ConnectedPlayer player : players.values()) {
      VelocityServerConnection serverConnection = player.getConnectedServer();
      if (serverConnection != null && serverConnection.getConnection() != null
              && serverConnection.server() == this) {
        return serverConnection.sendPluginMessage(identifier, data);
      }
    }

    data.release();
    return false;
  }

  @Override
  public String toString() {
    return "registered server: " + serverInfo;
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return this.players();
  }
}
