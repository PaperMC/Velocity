package io.minimum.minecraft.velocity.proxy;

import io.minimum.minecraft.velocity.data.ServerInfo;
import io.minimum.minecraft.velocity.protocol.packets.Chat;
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

    public void handleConnectionException(ServerInfo info, Throwable throwable) {
        String error = String.format("%s: %s",
                throwable.getClass().getName(), throwable.getMessage());
        Disconnect disconnect = new Disconnect();
        disconnect.setReason(ComponentSerializers.JSON.serialize(TextComponent.of(error, TextColor.RED)));
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
            Disconnect d = new Disconnect();
            d.setReason(ComponentSerializers.JSON.serialize(component));
            connection.closeWith(d);
        } else {
            Chat chat = new Chat();
            chat.setMessage(ComponentSerializers.JSON.serialize(component));
            connection.write(chat);
        }
    }

    public void setConnectedServer(ServerConnection serverConnection) {
        this.connectedServer = serverConnection;
    }
}
