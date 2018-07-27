package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.config.IPForwardingMode;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.protocol.packets.Handshake;
import com.velocitypowered.proxy.protocol.packets.ServerLogin;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.TimeUnit;

import static com.velocitypowered.network.Connections.FRAME_DECODER;
import static com.velocitypowered.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.network.Connections.HANDLER;
import static com.velocitypowered.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.network.Connections.READ_TIMEOUT;
import static com.velocitypowered.network.Connections.SERVER_READ_TIMEOUT_SECONDS;

public class ServerConnection implements MinecraftConnectionAssociation {
    private final ServerInfo serverInfo;
    private final ConnectedPlayer proxyPlayer;
    private final VelocityServer server;
    private MinecraftConnection channel;

    public ServerConnection(ServerInfo target, ConnectedPlayer proxyPlayer, VelocityServer server) {
        this.serverInfo = target;
        this.proxyPlayer = proxyPlayer;
        this.server = server;
    }

    public void connect() {
        server.initializeGenericBootstrap()
                .handler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(READ_TIMEOUT, new ReadTimeoutHandler(SERVER_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                                .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolConstants.Direction.TO_CLIENT))
                                .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolConstants.Direction.TO_SERVER));

                        MinecraftConnection connection = new MinecraftConnection(ch);
                        connection.setState(StateRegistry.HANDSHAKE);
                        connection.setSessionHandler(new LoginSessionHandler(ServerConnection.this));
                        connection.setAssociation(ServerConnection.this);
                        ch.pipeline().addLast(HANDLER, connection);
                    }
                })
                .connect(serverInfo.getAddress())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            channel = future.channel().pipeline().get(MinecraftConnection.class);

                            // Kick off the connection process
                            startHandshake();
                        } else {
                            proxyPlayer.handleConnectionException(serverInfo, future.cause());
                        }
                    }
                });
    }

    private String createBungeeForwardingAddress() {
        // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
        // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
        // UUID (undashed), and if you are in online-mode, their login properties (retrieved from Mojang).
        return serverInfo.getAddress().getHostString() + "\0" +
                proxyPlayer.getRemoteAddress().getHostString() + "\0" +
                proxyPlayer.getProfile().getId() + "\0" +
                VelocityServer.GSON.toJson(proxyPlayer.getProfile().getProperties());
    }

    private void startHandshake() {
        // Initiate a handshake.
        Handshake handshake = new Handshake();
        handshake.setNextStatus(2); // login
        handshake.setProtocolVersion(proxyPlayer.getConnection().getProtocolVersion());
        if (VelocityServer.getServer().getConfiguration().getIpForwardingMode() == IPForwardingMode.LEGACY) {
            handshake.setServerAddress(createBungeeForwardingAddress());
        } else {
            handshake.setServerAddress(serverInfo.getAddress().getHostString());
        }
        handshake.setPort(serverInfo.getAddress().getPort());
        channel.write(handshake);

        channel.setProtocolVersion(proxyPlayer.getConnection().getProtocolVersion());
        channel.setState(StateRegistry.LOGIN);

        // Login
        ServerLogin login = new ServerLogin();
        login.setUsername(proxyPlayer.getUsername());
        channel.write(login);
    }

    public ConnectedPlayer getProxyPlayer() {
        return proxyPlayer;
    }

    public MinecraftConnection getChannel() {
        return channel;
    }

    public ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void disconnect() {
        channel.close();
        channel = null;
    }

    @Override
    public String toString() {
        return "[server connection] " + proxyPlayer.getProfile().getName() + " -> " + serverInfo.getName();
    }
}
