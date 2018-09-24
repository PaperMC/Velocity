package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.title.Title;
import java.util.List;

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
     * Returns the player settings
     * @return the settings
     */
    PlayerSettings getPlayerSettings();

    /**
     * Returns the current player's ping
     * @return the player's ping or -1 if ping information is currently unknown
     */
    long getPing();
    
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
     * @param server the server to connect to
     * @return a new connection request
     */
    ConnectionRequestBuilder createConnectionRequest(@NonNull RegisteredServer server);

    /**
     * Gets a game profile properties of player
     * @return a immutable list of properties
     */
    List<GameProfile.Property> getGameProfileProperties();
    
    /**
     * Sets a GameProfile properties({@link GameProfile.Property)
     * @param properties a properties to set
     */
    void setGameProfileProperties(List<GameProfile.Property> properties);
    
    /**
     * Sets the tab list header and footer for the player.
     * @param header the header component
     * @param footer the footer component
     */
    void setHeaderAndFooter(Component header, Component footer);

    /**
     * Clears the tab list header and footer for the player.
     */
    void clearHeaderAndFooter();

    /**
     * Disconnects the player with the specified reason. Once this method is called, further calls to other {@link Player}
     * methods will become undefined.
     * @param reason component with the reason
     */
    void disconnect(Component reason);

    /**
     * Sends the specified title to the client.
     * @param title the title to send
     */
    void sendTitle(Title title);

    /**
     * Sends chat input onto the players current server as if they typed it
     * into the client chat box.
     * @param input the chat input to send
     */
    void spoofChatInput(String input);
}
