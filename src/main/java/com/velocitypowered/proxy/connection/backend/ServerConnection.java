package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.protocol.packets.Handshake;
import com.velocitypowered.proxy.protocol.packets.ServerLogin;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.protocol.netty.MinecraftPipelineUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
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

    private void startHandshake() {
        // Initiate a handshake.
        Handshake handshake = new Handshake();
        handshake.setNextStatus(2); // login
        handshake.setProtocolVersion(proxyPlayer.getConnection().getProtocolVersion());
        handshake.setServerAddress(serverInfo.getAddress().getHostString());
        handshake.setPort(serverInfo.getAddress().getPort());
        channel.write(handshake);

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
