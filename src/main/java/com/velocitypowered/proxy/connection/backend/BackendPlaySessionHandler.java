package com.velocitypowered.proxy.connection.backend;

import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packets.Disconnect;
import com.velocitypowered.proxy.protocol.packets.JoinGame;
import com.velocitypowered.proxy.protocol.packets.KeepAlive;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.packets.Respawn;
import io.netty.buffer.ByteBuf;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {
    private final ServerConnection connection;

    public BackendPlaySessionHandler(ServerConnection connection) {
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof KeepAlive) {
            // Forward onto the server
            connection.getChannel().write(packet);
        } else if (packet instanceof Disconnect) {
            Disconnect original = (Disconnect) packet;
            connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), original);
        } else if (packet instanceof JoinGame) {
            ClientPlaySessionHandler playerHandler =
                    (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
            playerHandler.handleBackendJoinGame((JoinGame) packet);
        } else if (packet instanceof Respawn) {
            // Record the dimension switch, and then forward the packet on.
            ClientPlaySessionHandler playerHandler =
                    (ClientPlaySessionHandler) connection.getProxyPlayer().getConnection().getSessionHandler();
            playerHandler.setCurrentDimension(((Respawn) packet).getDimension());
            connection.getProxyPlayer().getConnection().write(packet);
        } else {
            // Just forward the packet on. We don't have anything to handle at this time.
            connection.getProxyPlayer().getConnection().write(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        connection.getProxyPlayer().getConnection().write(buf.retain());
    }

    @Override
    public void exception(Throwable throwable) {
        connection.getProxyPlayer().handleConnectionException(connection.getServerInfo(), throwable);
    }
}
