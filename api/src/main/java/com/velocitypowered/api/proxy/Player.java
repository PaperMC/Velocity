package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.server.ServerInfo;
import com.velocitypowered.api.util.MessagePosition;
import net.kyori.text.Component;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a player who is connected to the proxy.
 */
public interface Player extends CommandSource, InboundConnection {
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
    Optional<ServerInfo> getCurrentServer();
    
    /**
     * Sends a chat message to the player's client.
     * @param component the chat message to send
     */
    default void sendMessage(@Nonnull Component component) {
        sendMessage(component, MessagePosition.CHAT);
    }

    /**
     * Sends a chat message to the player's client in the specified position.
     * @param component the chat message to send
     * @param position the position for the message
     */
    void sendMessage(@Nonnull Component component, @Nonnull MessagePosition position);

    /**
     * Creates a new connection request so that the player can connect to another server.
     * @param info the server to connect to
     * @return a new connection request
     */
    ConnectionRequestBuilder createConnectionRequest(@Nonnull ServerInfo info);
}
