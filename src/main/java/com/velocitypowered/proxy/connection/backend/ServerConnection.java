package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.config.IPForwardingMode;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.data.GameProfile;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.protocol.packets.Handshake;
import com.velocitypowered.proxy.protocol.packets.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packets.ServerLogin;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.concurrent.ThreadLocalRandom;
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
                                .addLast(MINECRAFT_DECODER, new MinecraftDecoder(ProtocolConstants.Direction.CLIENTBOUND))
                                .addLast(MINECRAFT_ENCODER, new MinecraftEncoder(ProtocolConstants.Direction.SERVERBOUND));

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

        int protocolVersion = proxyPlayer.getConnection().getProtocolVersion();
        channel.setProtocolVersion(protocolVersion);
        channel.setState(StateRegistry.LOGIN);

        // 1.13 stuff
        if (protocolVersion >= ProtocolConstants.MINECRAFT_1_13) {
            if (VelocityServer.getServer().getConfiguration().getIpForwardingMode() == IPForwardingMode.MODERN) {
                // Velocity's IP forwarding includes the player's IP address and their game profile.
                GameProfile profile = proxyPlayer.getProfile();
                ByteBuf buf = createForwardingData(proxyPlayer.getRemoteAddress().getHostString(), profile);

                // Send the message on
                LoginPluginMessage forwarding = new LoginPluginMessage();
                forwarding.setId(ThreadLocalRandom.current().nextInt());
                forwarding.setChannel(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL);
                forwarding.setData(buf);

                LoginSessionHandler lsh = (LoginSessionHandler) channel.getSessionHandler();
                lsh.setForwardingPacketId(forwarding.getId());
                channel.write(forwarding);
            }
        }

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

    private static ByteBuf createForwardingData(String address, GameProfile profile) {
        ByteBuf buf = Unpooled.buffer();
        ProtocolUtils.writeString(buf, address);
        ProtocolUtils.writeString(buf, profile.getName());
        ProtocolUtils.writeUuid(buf, profile.idAsUuid());
        ProtocolUtils.writeVarInt(buf, profile.getProperties().size());
        for (GameProfile.Property property : profile.getProperties()) {
            ProtocolUtils.writeString(buf, property.getName());
            ProtocolUtils.writeString(buf, property.getValue());
            String signature = property.getSignature();
            if (signature != null) {
                buf.writeBoolean(true);
                ProtocolUtils.writeString(buf, signature);
            } else {
                buf.writeBoolean(false);
            }
        }
        return buf;
    }
}
