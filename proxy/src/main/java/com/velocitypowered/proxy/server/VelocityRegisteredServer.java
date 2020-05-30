package com.velocitypowered.proxy.server;

import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.HANDLER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoop;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityRegisteredServer implements RegisteredServer {

  private final @Nullable VelocityServer server;
  private final ServerInfo serverInfo;
  private final Set<ConnectedPlayer> players = ConcurrentHashMap.newKeySet();

  public VelocityRegisteredServer(@Nullable VelocityServer server, ServerInfo serverInfo) {
    this.server = server;
    this.serverInfo = Preconditions.checkNotNull(serverInfo, "serverInfo");
  }

  @Override
  public ServerInfo getServerInfo() {
    return serverInfo;
  }

  @Override
  public Collection<Player> getPlayersConnected() {
    return ImmutableList.copyOf(players);
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
    if (server == null) {
      throw new IllegalStateException("No Velocity proxy instance available");
    }
    CompletableFuture<ServerPing> pingFuture = new CompletableFuture<>();
    server.createBootstrap(loop)
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
                .addLast(READ_TIMEOUT,
                    new ReadTimeoutHandler(server.getConfiguration().getReadTimeout(),
                        TimeUnit.MILLISECONDS))
                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                .addLast(MINECRAFT_DECODER,
                    new MinecraftDecoder(ProtocolUtils.Direction.CLIENTBOUND))
                .addLast(MINECRAFT_ENCODER,
                    new MinecraftEncoder(ProtocolUtils.Direction.SERVERBOUND));

            ch.pipeline().addLast(HANDLER, new MinecraftConnection(ch, server));
          }
        })
        .connect(serverInfo.getAddress())
        .addListener(new ChannelFutureListener() {
          @Override
          public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
              MinecraftConnection conn = future.channel().pipeline().get(MinecraftConnection.class);
              conn.setSessionHandler(new PingSessionHandler(
                  pingFuture, VelocityRegisteredServer.this, conn, version));
            } else {
              pingFuture.completeExceptionally(future.cause());
            }
          }
        });
    return pingFuture;
  }

  public void addPlayer(ConnectedPlayer player) {
    players.add(player);
  }

  public void removePlayer(ConnectedPlayer player) {
    players.remove(player);
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    return sendPluginMessage(identifier, Unpooled.wrappedBuffer(data));
  }

  /**
   * Sends a plugin message to the server through this connection.
   * @param identifier the channel ID to use
   * @param data the data
   * @return whether or not the message was sent
   */
  public boolean sendPluginMessage(ChannelIdentifier identifier, ByteBuf data) {
    for (ConnectedPlayer player : players) {
      VelocityServerConnection connection = player.getConnectedServer();
      if (connection != null && connection.getServerInfo().equals(serverInfo)) {
        return connection.sendPluginMessage(identifier, data);
      }
    }

    return false;
  }

  @Override
  public String toString() {
    return "registered server: " + serverInfo;
  }
}
