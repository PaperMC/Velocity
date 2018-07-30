package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packets.Disconnect;
import com.velocitypowered.proxy.protocol.packets.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packets.ServerLoginSuccess;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packets.SetCompression;

public class LoginSessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;

    public LoginSessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof EncryptionRequest) {
            throw new IllegalStateException("Backend server is online-mode!");
        }

        if (packet instanceof Disconnect) {
            Disconnect disconnect = (Disconnect) packet;
            connection.disconnect();
            connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), disconnect);
        }

        if (packet instanceof SetCompression) {
            SetCompression sc = (SetCompression) packet;
            connection.getChannel().setCompressionThreshold(sc.getThreshold());
        }

        if (packet instanceof ServerLoginSuccess) {
            // The player has been logged on to the backend server.
            connection.getChannel().setState(StateRegistry.PLAY);
            ServerConnection existingConnection = connection.getProxyPlayer().getConnectedServer();
            if (existingConnection == null) {
                // Strap on the play session handler
                connection.getProxyPlayer().getConnection().setSessionHandler(new ClientPlaySessionHandler(connection.getProxyPlayer()));
            } else {
                // The previous server connection should become obsolete.
                existingConnection.disconnect();
            }
            connection.getChannel().setSessionHandler(new BackendPlaySessionHandler(connection));
            connection.getProxyPlayer().setConnectedServer(connection);
        }
    }

    @Override
    public void exception(Throwable throwable) {
        connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), throwable);
    }
}
