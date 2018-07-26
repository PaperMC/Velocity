package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.protocol.packets.Chat;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.util.ThrowableUtils;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.protocol.packets.Disconnect;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;

import java.net.InetSocketAddress;
import java.util.UUID;

public class ConnectedPlayer {
    private final String username;
    private final UUID uniqueId;
    private final MinecraftConnection connection;
    private ServerConnection connectedServer;

    public ConnectedPlayer(String username, UUID uniqueId, MinecraftConnection connection) {
        this.username = username;
        this.uniqueId = uniqueId;
        this.connection = connection;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public MinecraftConnection getConnection() {
        return connection;
    }

    public InetSocketAddress getRemoteAddress() {
        return (InetSocketAddress) connection.getChannel().remoteAddress();
    }

    public ServerConnection getConnectedServer() {
        return connectedServer;
    }

    public void handleConnectionException(ServerInfo info, Throwable throwable) {
        String error = ThrowableUtils.briefDescription(throwable);
        Disconnect disconnect = Disconnect.create(TextComponent.of(error, TextColor.RED));
        handleConnectionException(info, disconnect);
    }

    public void handleConnectionException(ServerInfo info, Disconnect disconnect) {
        TextComponent component = TextComponent.builder()
                .content("Exception connecting to server " + info.getName() + ": ")
                .color(TextColor.RED)
                .append(ComponentSerializers.JSON.deserialize(disconnect.getReason()))
                .build();

        if (connectedServer == null) {
            // The player isn't yet connected to a server - we should disconnect them.
            connection.closeWith(Disconnect.create(component));
        } else {
            connection.write(Chat.create(component));
        }
    }

    public void setConnectedServer(ServerConnection serverConnection) {
        this.connectedServer = serverConnection;
    }

    public void close(TextComponent reason) {
        connection.closeWith(Disconnect.create(reason));
    }
}
