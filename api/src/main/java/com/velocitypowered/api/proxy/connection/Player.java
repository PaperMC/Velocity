/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.connection;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import com.velocitypowered.api.proxy.messages.ChannelMessageSource;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.api.proxy.player.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.player.PlatformActions;
import com.velocitypowered.api.proxy.player.PlayerIdentity;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.player.java.JavaClientSettings;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.ModInfo;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a player who is connected to the proxy.
 */
public interface Player extends CommandSource, Identified, InboundConnection,
    ChannelMessageSource, ChannelMessageSink, PlatformActions {

  /**
   * Returns the player's current username.
   *
   * @return the username
   */
  String username();

  /**
   * Returns the player's UUID.
   *
   * @return the UUID
   */
  UUID id();

  /**
   * Returns the server that the player is currently connected to.
   *
   * @return the server that the player is connected to, which could be {@code null} if no
   *         connection was made yet (or the player is switching servers)
   */
  @Nullable ServerConnection connectedServer();

  /**
   * Returns the player's client settings.
   *
   * @return the settings
   */
  JavaClientSettings clientSettings();

  /**
   * Returns the player's mod info if they have a modded client.
   *
   * @return an the mod info. which may be {@code null} if no info is available
   */
  @Nullable ModInfo modInfo();

  /**
   * Returns the current player's ping.
   *
   * @return the player's ping or -1 if ping information is currently unknown
   */
  long ping();

  /**
   * Returns the player's connection status.
   *
   * @return true if the player is authenticated with Mojang servers
   */
  boolean onlineMode();

  /**
   * Creates a new connection request so that the player can connect to another server.
   *
   * @param server the server to connect to
   * @return a new connection request
   */
  ConnectionRequestBuilder createConnectionRequest(RegisteredServer server);

  /**
   * Sets the player's profile properties.
   *
   * @param properties the properties
   */
  void setGameProfileProperties(List<JavaPlayerIdentity.Property> properties);

  /**
   * Returns the player's identity, which depends on what version of Minecraft they are currently
   * playing.
   */
  @Override
  @NonNull PlayerIdentity identity();

  /**
   * Returns the player's tab list.
   *
   * @return this player's tab list
   */
  TabList tabList();

  /**
   * Disconnects the player with the specified reason. Once this method is called, further calls to
   * other {@link Player} methods will become undefined.
   *
   * @param reason component with the reason
   */
  void disconnect(Component reason);

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
   * {@link #connectedServer()}.
   *
   * {@inheritDoc}
   */
  @Override
  boolean sendPluginMessage(PluginChannelId identifier, byte[] data);
}
