package com.velocitypowered.proxy.connection.backend;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.AttributeKey;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.velocitypowered.proxy.VelocityServer.GSON;
import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.HANDLER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

public class VelocityServerConnection implements MinecraftConnectionAssociation, ServerConnection {
    static final AttributeKey<CompletableFuture<ConnectionRequestBuilder.Result>> CONNECTION_NOTIFIER =
            AttributeKey.newInstance("connection-notification-result");

    private final VelocityRegisteredServer registeredServer;
    private final ConnectedPlayer proxyPlayer;
    private final VelocityServer server;
    private MinecraftConnection minecraftConnection;
    private boolean legacyForge = false;
    private boolean hasCompletedJoin = false;
    private boolean gracefulDisconnect = false;

    public VelocityServerConnection(VelocityRegisteredServer registeredServer, ConnectedPlayer proxyPlayer, VelocityServer server) {
        this.registeredServer = registeredServer;
        this.proxyPlayer = proxyPlayer;
        this.server = server;
    }

    public CompletableFuture<ConnectionRequestBuilder.Result> connect() {
        CompletableFuture<ConnectionRequestBuilder.Result> result = new CompletableFuture<>();
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

                        ch.attr(CONNECTION_NOTIFIER).set(result);
                        MinecraftConnection connection = new MinecraftConnection(ch, server);
                        connection.setState(StateRegistry.HANDSHAKE);
                        connection.setAssociation(VelocityServerConnection.this);
                        ch.pipeline().addLast(HANDLER, connection);
                    }
                })
                .connect(registeredServer.getServerInfo().getAddress())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            minecraftConnection = future.channel().pipeline().get(MinecraftConnection.class);

                            // Kick off the connection process
                            minecraftConnection.setSessionHandler(new LoginSessionHandler(server, VelocityServerConnection.this));
                            startHandshake();
                        } else {
                            result.completeExceptionally(future.cause());
                        }
                    }
                });
        return result;
    }

    private String createBungeeForwardingAddress() {
        // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
        // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
        // UUID (undashed), and if you are in online-mode, their login properties (retrieved from Mojang).
        return registeredServer.getServerInfo().getAddress().getHostString() + "\0" +
                proxyPlayer.getRemoteAddress().getHostString() + "\0" +
                proxyPlayer.getProfile().getId() + "\0" +
                GSON.toJson(proxyPlayer.getProfile().getProperties());
    }

    private void startHandshake() {
        PlayerInfoForwarding forwardingMode = server.getConfiguration().getPlayerInfoForwardingMode();

        // Initiate a handshake.
        Handshake handshake = new Handshake();
        handshake.setNextStatus(StateRegistry.LOGIN_ID);
        handshake.setProtocolVersion(proxyPlayer.getConnection().getProtocolVersion());
        if (forwardingMode == PlayerInfoForwarding.LEGACY) {
            handshake.setServerAddress(createBungeeForwardingAddress());
        } else if (proxyPlayer.getConnection().isLegacyForge()) {
            handshake.setServerAddress(handshake.getServerAddress() + "\0FML\0");
        } else {
            handshake.setServerAddress(registeredServer.getServerInfo().getAddress().getHostString());
        }
        handshake.setPort(registeredServer.getServerInfo().getAddress().getPort());
        minecraftConnection.write(handshake);

        int protocolVersion = proxyPlayer.getConnection().getProtocolVersion();
        minecraftConnection.setProtocolVersion(protocolVersion);
        minecraftConnection.setState(StateRegistry.LOGIN);

        ServerLogin login = new ServerLogin();
        login.setUsername(proxyPlayer.getUsername());
        minecraftConnection.write(login);
    }

    public void writeIfJoined(PluginMessage message) {
        if (hasCompletedJoin) {
            minecraftConnection.write(message);
        }
    }

    public MinecraftConnection getMinecraftConnection() {
        return minecraftConnection;
    }

    @Override
    public VelocityRegisteredServer getServer() {
        return registeredServer;
    }

    @Override
    public ServerInfo getServerInfo() {
        return registeredServer.getServerInfo();
    }

    @Override
    public ConnectedPlayer getPlayer() {
        return proxyPlayer;
    }

    public void disconnect() {
        if (minecraftConnection != null) {
            minecraftConnection.close();
            minecraftConnection = null;
            gracefulDisconnect = true;
        }
    }

    @Override
    public String toString() {
        return "[server connection] " + proxyPlayer.getProfile().getName() + " -> " + registeredServer.getServerInfo().getName();
    }

    @Override
    public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
        Preconditions.checkNotNull(identifier, "identifier");
        Preconditions.checkNotNull(data, "data");
        PluginMessage message = new PluginMessage();
        message.setChannel(identifier.getId());
        message.setData(data);
        minecraftConnection.write(message);
        return true;
    }

    public boolean isLegacyForge() {
        return legacyForge;
    }

    public void setLegacyForge(boolean modded) {
        legacyForge = modded;
    }

    public boolean hasCompletedJoin() {
        return hasCompletedJoin;
    }

    public void setHasCompletedJoin(boolean hasCompletedJoin) {
        this.hasCompletedJoin = hasCompletedJoin;
    }

    public boolean isGracefulDisconnect() {
        return gracefulDisconnect;
    }
}
