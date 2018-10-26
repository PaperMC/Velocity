package com.velocitypowered.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.server.ping.PingSessionHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.network.Connections.*;

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
        if (server == null) {
            throw new IllegalStateException("No Velocity proxy instance available");
        }
        CompletableFuture<ServerPing> pingFuture = new CompletableFuture<>();
        server.initializeGenericBootstrap()
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(READ_TIMEOUT, new ReadTimeoutHandler(server.getConfiguration().getReadTimeout(), TimeUnit.SECONDS))
                                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                                .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolConstants.Direction.CLIENTBOUND))
                                .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolConstants.Direction.SERVERBOUND));

                        MinecraftConnection connection = new MinecraftConnection(ch, server);
                        connection.setState(StateRegistry.HANDSHAKE);
                        ch.pipeline().addLast(HANDLER, connection);
                    }
                })
                .connect(serverInfo.getAddress())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            MinecraftConnection conn = future.channel().pipeline().get(MinecraftConnection.class);
                            conn.setSessionHandler(new PingSessionHandler(pingFuture, VelocityRegisteredServer.this, conn));
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
        for (ConnectedPlayer player : players) {
            ServerConnection connection = player.getConnectedServer();
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
