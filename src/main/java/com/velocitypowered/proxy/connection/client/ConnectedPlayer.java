package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.data.GameProfile;
import com.velocitypowered.proxy.protocol.packets.Chat;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.backend.ServerConnection;
import com.velocitypowered.proxy.protocol.packets.ClientSettings;
import com.velocitypowered.proxy.util.ThrowableUtils;
import com.velocitypowered.proxy.data.ServerInfo;
import com.velocitypowered.proxy.protocol.packets.Disconnect;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;
import net.kyori.text.serializer.PlainComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.UUID;

public class ConnectedPlayer implements MinecraftConnectionAssociation {
    private static final PlainComponentSerializer PASS_THRU_TRANSLATE = new PlainComponentSerializer((c) -> "", TranslatableComponent::key);

    private static final Logger logger = LogManager.getLogger(ConnectedPlayer.class);

    private final GameProfile profile;
    private final MinecraftConnection connection;
    private ServerConnection connectedServer;
    private ClientSettings clientSettings;

    public ConnectedPlayer(GameProfile profile, MinecraftConnection connection) {
        this.profile = profile;
        this.connection = connection;
    }

    public String getUsername() {
        return profile.getName();
    }

    public UUID getUniqueId() {
        return profile.idAsUuid();
    }

    public GameProfile getProfile() {
        return profile;
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

    public ClientSettings getClientSettings() {
        return clientSettings;
    }

    public void setClientSettings(ClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }

    public void handleConnectionException(ServerInfo info, Throwable throwable) {
        String error = ThrowableUtils.briefDescription(throwable);
        String userMessage;
        if (connectedServer != null && connectedServer.getServerInfo().equals(info)) {
            logger.error("{}: exception occurred in connection to {}", this, info.getName(), throwable);
            userMessage = "Exception in server " + info.getName();
        } else {
            logger.error("{}: unable to connect to server {}", this, info.getName(), throwable);
            userMessage = "Exception connecting to server " + info.getName();
        }
        handleConnectionException(info, TextComponent.builder()
                .content(userMessage + ": ")
                .color(TextColor.RED)
                .append(TextComponent.of(error, TextColor.WHITE))
                .build());
    }

    public void handleConnectionException(ServerInfo info, Disconnect disconnect) {
        Component disconnectReason = ComponentSerializers.JSON.deserialize(disconnect.getReason());
        String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
        if (connectedServer != null && connectedServer.getServerInfo().equals(info)) {
            logger.error("{}: kicked from server {}: {}", this, info.getName(), plainTextReason);
        } else {
            logger.error("{}: disconnected while connecting to {}: {}", this, info.getName(), plainTextReason);
        }
        handleConnectionException(info, disconnectReason);
    }

    private void handleConnectionException(ServerInfo info, Component disconnectReason) {
        if (connectedServer == null || connectedServer.getServerInfo().equals(info)) {
            // The player isn't yet connected to a server or they are already connected to the server
            // they're disconnected from.
            connection.closeWith(Disconnect.create(disconnectReason));
        } else {
            connection.write(Chat.create(disconnectReason));
        }
    }

    public void setConnectedServer(ServerConnection serverConnection) {
        this.connectedServer = serverConnection;
    }

    public void close(TextComponent reason) {
        connection.closeWith(Disconnect.create(reason));
    }

    @Override
    public String toString() {
        return "[connected player] " + getProfile().getName() + " (" + getRemoteAddress() + ")";
    }
}
