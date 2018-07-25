package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.protocol.packets.Disconnect;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;

import java.util.UUID;

public class ConnectedPlayer {
    private final String username;
    private final UUID uniqueId;
    private final InboundMinecraftConnection connection;
    private ServerConnection connectedServer;

    public ConnectedPlayer(String username, UUID uniqueId, InboundMinecraftConnection connection) {
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

    public InboundMinecraftConnection getConnection() {
        return connection;
    }

    public ServerConnection getConnectedServer() {
        return connectedServer;
    }

    public void handleConnectionException(Throwable throwable) {
        String error = "Exception: " + throwable.getClass().getName() + ": " + throwable.getMessage();
        Disconnect disconnect = new Disconnect();
        disconnect.setReason(ComponentSerializers.JSON.serialize(TextComponent.of(error, TextColor.RED)));
        handleConnectionException(disconnect);
    }

    public void handleConnectionException(Disconnect disconnect) {
        if (connectedServer == null) {
            // The player isn't yet connected to a server - we should disconnect them.
            connection.closeWith(disconnect);
        } else {
            // TODO
        }
    }
}
