package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.server.ServerInfo;
import com.velocitypowered.api.util.MessagePosition;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player who is connected to the proxy.
 */
public interface Player extends CommandSource, InboundConnection, ChannelMessageSource, ChannelMessageSink {
    /**
     * Returns the player's current username.
     * @return the username
     */
    String getUsername();

    /**
     * Returns the player's UUID.
     * @return the UUID
     */
    UUID getUniqueId();

    /**
     * Returns the server that the player is currently connected to.
     * @return an {@link Optional} the server that the player is connected to, which may be empty
     */
    Optional<ServerConnection> getCurrentServer();
    
    /**
     * Sends a chat message to the player's client.
     * @param component the chat message to send
     */
    default void sendMessage(@NonNull Component component) {
        sendMessage(component, MessagePosition.CHAT);
    }

    /**
     * Sends a chat message to the player's client in the specified position.
     * @param component the chat message to send
     * @param position the position for the message
     */
    void sendMessage(@NonNull Component component, @NonNull MessagePosition position);

    /**
     * Creates a new connection request so that the player can connect to another server.
     * @param info the server to connect to
     * @return a new connection request
     */
    ConnectionRequestBuilder createConnectionRequest(@NonNull ServerInfo info);

    /**
     * Sets a header and footer to the player
     * @param header component with header
     * @param footer component with footer
     */
    void setHeaderAndFooter(@NonNull Component header, @NonNull Component footer);
    
    /**
     * Disconnects the player with the reason
     * @param reason component with the reason
     */
    void disconnect(Component reason);
}
