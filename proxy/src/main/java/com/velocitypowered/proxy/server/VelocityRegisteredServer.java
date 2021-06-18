/*
 * Copyright (C) 2018 Velocity Contributors
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

import static com.velocitypowered.proxy.network.HandlerNames.FRAME_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.HANDLER;
import static com.velocitypowered.proxy.network.HandlerNames.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.HandlerNames.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.HandlerNames.READ_TIMEOUT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.network.java.pipeline.MinecraftDecoder;
import com.velocitypowered.proxy.network.java.pipeline.MinecraftEncoder;
import com.velocitypowered.proxy.network.java.pipeline.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.network.java.pipeline.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityRegisteredServer implements RegisteredServer, ForwardingAudience {

  private final @Nullable VelocityServer instance;
  private final ServerInfo serverInfo;
  private final Map<UUID, ConnectedPlayer> players = new ConcurrentHashMap<>();

  public VelocityRegisteredServer(@Nullable VelocityServer instance, ServerInfo serverInfo) {
    this.instance = instance;
    this.serverInfo = Preconditions.checkNotNull(serverInfo, "serverInfo");
  }

  @Override
  public ServerInfo serverInfo() {
    return serverInfo;
  }

  @Override
  public Collection<Player> connectedPlayers() {
    return ImmutableList.copyOf(players.values());
  }

  @Override
  public CompletableFuture<ServerPing> ping() {
    return ping(null, ProtocolVersion.UNKNOWN);
  }

  /**
   * Pings the specified server using the specified event {@code loop}, claiming to be
   * {@code version}.
   * @param loop the event loop to use
   * @param version the version to report
   * @return the server list ping response
   */
  public CompletableFuture<ServerPing> ping(@Nullable EventLoop loop, ProtocolVersion version) {
    VelocityServer instance = this.instance;
    if (instance == null) {
      throw new IllegalStateException("No Velocity proxy instance available");
    }
    CompletableFuture<ServerPing> pingFuture = new CompletableFuture<>();
    instance.createBootstrap(loop, serverInfo.address())
        .handler(new ChannelInitializer<>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                .addLast(READ_TIMEOUT,
                    new ReadTimeoutHandler(instance.configuration().getReadTimeout(),
                        TimeUnit.MILLISECONDS))
                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                .addLast(MINECRAFT_DECODER,
                    new MinecraftDecoder(PacketDirection.CLIENTBOUND))
                .addLast(MINECRAFT_ENCODER,
                    new MinecraftEncoder(PacketDirection.SERVERBOUND));

            ch.pipeline().addLast(HANDLER, new MinecraftConnection(ch, instance));
          }
        })
        .connect(serverInfo.address())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            MinecraftConnection conn = future.channel().pipeline().get(MinecraftConnection.class);
            conn.setSessionHandler(new PingSessionHandler(
                pingFuture, VelocityRegisteredServer.this, conn, version));
          } else {
            pingFuture.completeExceptionally(future.cause());
          }
        });
    return pingFuture;
  }

  public void addPlayer(ConnectedPlayer player) {
    players.put(player.id(), player);
  }

  public void removePlayer(ConnectedPlayer player) {
    players.remove(player.id(), player);
  }

  @Override
  public boolean sendPluginMessage(PluginChannelId identifier, byte[] data) {
    return sendPluginMessage(identifier, Unpooled.wrappedBuffer(data));
  }

  /**
   * Sends a plugin message to the server through this connection. The message will be released
   * afterwards.
   *
   * @param identifier the channel ID to use
   * @param data the data
   * @return whether or not the message was sent
   */
  public boolean sendPluginMessage(PluginChannelId identifier, ByteBuf data) {
    for (ConnectedPlayer player : players.values()) {
      VelocityServerConnection connection = player.getConnectedServer();
      if (connection != null && connection.target() == this) {
        return connection.sendPluginMessage(identifier, data);
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
    return this.connectedPlayers();
  }
}
