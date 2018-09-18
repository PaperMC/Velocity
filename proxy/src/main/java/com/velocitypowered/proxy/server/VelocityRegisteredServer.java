package com.velocitypowered.proxy.server;

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

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.velocitypowered.proxy.network.Connections.*;
import static com.velocitypowered.proxy.network.Connections.HANDLER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;

public class VelocityRegisteredServer implements RegisteredServer {
    private final VelocityServer server;
    private final ServerInfo serverInfo;
    private final Set<ConnectedPlayer> players = ConcurrentHashMap.newKeySet();

    public VelocityRegisteredServer(VelocityServer server, ServerInfo serverInfo) {
        this.server = server;
        this.serverInfo = serverInfo;
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
            if (player.getConnectedServer() != null && player.getConnectedServer().getServerInfo().equals(serverInfo)) {
                ServerConnection connection = player.getConnectedServer();
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
