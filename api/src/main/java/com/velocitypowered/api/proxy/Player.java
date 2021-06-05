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
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.title.Title;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

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
   * Sends a chat message to the player's client.
   *
   * @param component the chat message to send
   * @deprecated Use {@link #sendMessage(Identified, Component)}
   *     or {@link #sendMessage(Identity, Component)} instead
   */
  @Deprecated
  @Override
  default void sendMessage(net.kyori.text.Component component) {
    sendMessage(component, MessagePosition.CHAT);
  }

  /**
   * Sends a chat message to the player's client in the specified position.
   *
   * @param component the chat message to send
   * @param position the position for the message
   * @deprecated Use @deprecated Use {@link #sendMessage(Identified, Component)} or
   *     {@link #sendMessage(Identity, Component)} for chat messages, or
   *     {@link #sendActionBar(net.kyori.adventure.text.Component)} for action bar messages
   */
  @Deprecated
  void sendMessage(net.kyori.text.Component component, MessagePosition position);

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
   * Sets the tab list header and footer for the player.
   *
   * @param header the header component
   * @param footer the footer component
   * @deprecated Use {@link TabList#setHeaderAndFooter(Component, Component)}.
   */
  @Deprecated
  void setHeaderAndFooter(net.kyori.text.Component header, net.kyori.text.Component footer);

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
   * @deprecated Use {@link #disconnect(Component)} instead
   */
  @Deprecated
  void disconnect(net.kyori.text.Component reason);

  /**
   * Disconnects the player with the specified reason. Once this method is called, further calls to
   * other {@link Player} methods will become undefined.
   *
   * @param reason component with the reason
   */
  void disconnect(net.kyori.adventure.text.Component reason);

  /**
   * Sends the specified title to the client.
   *
   * @param title the title to send
   * @deprecated Use {@link #showTitle(net.kyori.adventure.title.Title)} and {@link #resetTitle()}
   */
  @Deprecated
  void sendTitle(Title title);

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
   * @deprecated Use {@link #sendResourcePackOffer(ResourcePackInfo)} instead
   */
  @Deprecated
  void sendResourcePack(String url);

  /**
   * Sends the specified resource pack from {@code url} to the user, using the specified 20-byte
   * SHA-1 hash. To monitor the status of the sent resource pack, subscribe to
   * {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   * @param hash the SHA-1 hash value for the resource pack
   * @deprecated Use {@link #sendResourcePackOffer(ResourcePackInfo)} instead
   */
  @Deprecated
  void sendResourcePack(String url, byte[] hash);

  /**
   * Queues and sends a new Resource-pack offer to the player.
   * To monitor the status of the sent resource pack, subscribe to
   * {@link PlayerResourcePackStatusEvent}.
   * To create a {@link ResourcePackInfo} use the
   * {@link ProxyServer#createResourcePackBuilder(String)} builder.
   *
   * @param packInfo the resource-pack in question
   */
  void sendResourcePackOffer(ResourcePackInfo packInfo);

  /**
   * Gets the {@link ResourcePackInfo} of the currently applied
   * resource-pack or null if none.
   *
   * @return the applied resource pack or null if none.
   */
  @Nullable
  ResourcePackInfo getAppliedResourcePack();

  /**
   * Gets the {@link ResourcePackInfo} of the resource pack
   * the user is currently downloading or is currently
   * prompted to install or null if none.
   *
   * @return the pending resource pack or null if none
   */
  @Nullable
  ResourcePackInfo getPendingResourcePack();

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
