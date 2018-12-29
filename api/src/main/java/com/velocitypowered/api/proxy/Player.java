package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.title.Title;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.text.Component;

/**
 * Represents a player who is connected to the proxy.
 */
public interface Player extends CommandSource, InboundConnection, ChannelMessageSource,
    ChannelMessageSink {

  /**
   * Returns the player's current username.
   *
   * @return the username
   */
  String getUsername();


  /**
   * Returns the player's UUID.
   *
   * @return the UUID
   */
  UUID getUniqueId();

  /**
   * Returns the server that the player is currently connected to.
   *
   * @return an {@link Optional} the server that the player is connected to, which may be empty
   */
  Optional<ServerConnection> getCurrentServer();

  /**
   * Returns the player's client settings.
   *
   * @return the settings
   */
  PlayerSettings getPlayerSettings();

  /**
   * Returns the player's mod info if they have a modded client.
   *
   * @return an {@link Optional} the mod info. which may be empty
   */
  Optional<ModInfo> getModInfo();

  /**
   * Returns the current player's ping.
   *
   * @return the player's ping or -1 if ping information is currently unknown
   */
  long getPing();

  /**
   * Sends a chat message to the player's client.
   *
   * @param component the chat message to send
   */
  default void sendMessage(Component component) {
    sendMessage(component, MessagePosition.CHAT);
  }

  /**
   * Sends a chat message to the player's client in the specified position.
   *
   * @param component the chat message to send
   * @param position the position for the message
   */
  void sendMessage(Component component, MessagePosition position);

  /**
   * Creates a new connection request so that the player can connect to another server.
   *
   * @param server the server to connect to
   * @return a new connection request
   */
  ConnectionRequestBuilder createConnectionRequest(RegisteredServer server);

  /**
   * Gets the player's profile properties.
   *
   * <p>The returned list may be unmodifiable.</p>
   *
   * @return the player's profile properties
   */
  List<GameProfile.Property> getGameProfileProperties();

  /**
   * Sets the player's profile properties.
   *
   * @param properties the properties
   */
  void setGameProfileProperties(List<GameProfile.Property> properties);

  /**
   * Sets the tab list header and footer for the player.
   *
   * @param header the header component
   * @param footer the footer component
   * @deprecated Use {@link TabList#setHeaderAndFooter(Component, Component)}.
   */
  @Deprecated
  void setHeaderAndFooter(Component header, Component footer);

  /**
   * Clears the tab list header and footer for the player.
   *
   * @deprecated Use {@link TabList#clearHeaderAndFooter()}.
   */
  @Deprecated
  void clearHeaderAndFooter();

  /**
   * Returns the player's tab list.
   *
   * @return this player's tab list
   */
  TabList getTabList();

  /**
   * Disconnects the player with the specified reason. Once this method is called, further calls to
   * other {@link Player} methods will become undefined.
   *
   * @param reason component with the reason
   */
  void disconnect(Component reason);

  /**
   * Sends the specified title to the client.
   *
   * @param title the title to send
   */
  void sendTitle(Title title);

  /**
   * Sends chat input onto the players current server as if they typed it into the client chat box.
   *
   * @param input the chat input to send
   */
  void spoofChatInput(String input);

  /**
   * Sends the specified resource pack from {@code url} to the user. If at all possible, send the
   * resource pack using {@link #sendResourcePack(String, byte[])}.
   *
   * @param url the URL for the resource pack
   */
  void sendResourcePack(String url);

  /**
   * Sends the specified resource pack from {@code url} to the user.
   *
   * @param url the URL for the resource pack
   * @param hash the SHA-1 hash value for the resource pack
   */
  void sendResourcePack(String url, byte[] hash);
}
