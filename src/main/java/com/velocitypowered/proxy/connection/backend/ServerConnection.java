package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.protocol.packets.Handshake;
import com.velocitypowered.proxy.protocol.packets.ServerLogin;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.protocol.netty.MinecraftPipelineUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.util.UuidUtils;
import io.netty.channel.*;

public class ServerConnection {
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
                        MinecraftPipelineUtils.strapPipelineForBackend(ch);

                        MinecraftConnection connection = new MinecraftConnection(ch);
                        connection.setState(StateRegistry.HANDSHAKE);
                        connection.setSessionHandler(new LoginSessionHandler(ServerConnection.this));
                        ch.pipeline().addLast("handler", connection);
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
        //
        // Velocity doesn't yet support online-mode, unfortunately. That will come soon.
        return serverInfo.getAddress().getHostString() + "\0" +
                proxyPlayer.getRemoteAddress().getHostString() + "\0" +
                UuidUtils.toUndashed(proxyPlayer.getUniqueId());
    }

    private void startHandshake() {
        // Initiate a handshake.
        Handshake handshake = new Handshake();
        handshake.setNextStatus(2); // login
        handshake.setProtocolVersion(proxyPlayer.getConnection().getProtocolVersion());
        handshake.setServerAddress(createBungeeForwardingAddress());
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
}
