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

import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.HANDLER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RegisteredServerImpl implements RegisteredServer, ForwardingAudience {

  private final @Nullable VelocityServer server;
  private final ServerInfo serverInfo;
  private final DefaultMutableServerState state = new DefaultMutableServerState();

  public RegisteredServerImpl(@Nullable VelocityServer server, ServerInfo serverInfo) {
    this.server = server;
    this.serverInfo = Preconditions.checkNotNull(serverInfo, "serverInfo");
  }

  @Override
  public ServerInfo getServerInfo() {
    return serverInfo;
  }

  @Override
  public CompletableFuture<ServerPing> ping() {
    return ping(null, ProtocolVersion.UNKNOWN);
  }

  /**
   * Pings the specified server using the specified event {@code loop}, claiming to be
   * {@code version}.
   *
   * @param loop    the event loop to use
   * @param version the version to report
   * @return the server list ping response
   */
  public CompletableFuture<ServerPing> ping(@Nullable EventLoop loop, ProtocolVersion version) {
    if (server == null) {
      throw new IllegalStateException("No Velocity proxy instance available");
    }
    CompletableFuture<ServerPing> pingFuture = new CompletableFuture<>();
    server.createBootstrap(loop)
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                .addLast(READ_TIMEOUT,
                    new ReadTimeoutHandler(server.getConfiguration().getReadTimeout(),
                        TimeUnit.MILLISECONDS))
                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                .addLast(MINECRAFT_DECODER,
                    new MinecraftDecoder(ProtocolUtils.Direction.CLIENTBOUND))
                .addLast(MINECRAFT_ENCODER,
                    new MinecraftEncoder(ProtocolUtils.Direction.SERVERBOUND));

            ch.pipeline().addLast(HANDLER, new MinecraftConnection(ch, server));
          }
        })
        .connect(serverInfo.getAddress())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            MinecraftConnection conn = future.channel().pipeline().get(MinecraftConnection.class);
            conn.setSessionHandler(new PingSessionHandler(
                pingFuture, RegisteredServerImpl.this, conn, version));
          } else {
            pingFuture.completeExceptionally(future.cause());
          }
        });
    return pingFuture;
  }

  @Override
  public DefaultMutableServerState getState() {
    return state;
  }

  @Override
  public String toString() {
    return "registered server: " + serverInfo;
  }

  @Override
  public @NonNull Iterable<? extends Audience> audiences() {
    return this.getState().audiences();
  }
}
