/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;

/**
 * Represents a player who is connected to the proxy.
 */
public interface Player extends CommandSource, Identified, InboundConnection,
    ChannelMessageSource, ChannelMessageSink {

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
   * Returns the player's connection status.
   *
   * @return true if the player is authenticated with Mojang servers
   */
  boolean isOnlineMode();

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
   * Returns the player's game profile.
   */
  GameProfile getGameProfile();

  /**
   * Clears the tab list header and footer for the player.
   *
   * @deprecated Use {@link TabList#clearHeaderAndFooter()}.
   */
  @Deprecated
  void clearHeaderAndFooter();

  /**
   * Returns the player's player list header.
   *
   * @return this player's player list header
   */
  Component getPlayerListHeader();

  /**
   * Returns the player's player list footer.
   *
   * @return this player's tab list
   */
  Component getPlayerListFooter();

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
  void disconnect(net.kyori.adventure.text.Component reason);

  /**
   * Sends chat input onto the players current server as if they typed it into the client chat box.
   *
   * @param input the chat input to send
   */
  void spoofChatInput(String input);

  /**
   * Sends the specified resource pack from {@code url} to the user. If at all possible, send the
   * resource pack using {@link #sendResourcePack(String, byte[])}. To monitor the status of the
   * sent resource pack, subscribe to {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   */
  void sendResourcePack(String url);

  /**
   * Sends the specified resource pack from {@code url} to the user, using the specified 20-byte
   * SHA-1 hash. To monitor the status of the sent resource pack, subscribe to
   * {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   * @param hash the SHA-1 hash value for the resource pack
   */
  void sendResourcePack(String url, byte[] hash);

  /**
   * <strong>Note that this method does not send a plugin message to the server the player
   * is connected to.</strong> You should only use this method if you are trying to communicate
   * with a mod that is installed on the player's client. To send a plugin message to the server
   * from the player, you should use the equivalent method on the instance returned by
   * {@link #getCurrentServer()}.
   *
   * @inheritDoc
   */
  @Override
  boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data);
}
