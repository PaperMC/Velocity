/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.proxy.player.java.JavaClientSettings;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity.Property;
import com.velocitypowered.api.proxy.player.java.TabList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides certain actions that may be implemented across platforms (or not at all). Similar to
 * Adventure's {@link net.kyori.adventure.audience.Audience}, methods that are not implemented for
 * a platform will silently fail or return no-op instances as appropriate.
 */
public interface PlatformActions {

  /**
   * Sets the player's profile properties. This operation is not applicable to Bedrock Edition.
   *
   * @param properties the properties
   */
  void setGameProfileProperties(List<Property> properties);

  /**
   * Returns the player's client settings as sent by the client, for Java Edition clients only.
   *
   * @return the settings
   */
  @Nullable JavaClientSettings clientSettings();

  /**
   * Returns the player's tab list, if supported.
   *
   * @return this player's tab list
   */
  TabList tabList();

  /**
   * Sends the specified resource pack from {@code url} to the user, if supported. If possible,
   * send the  resource pack using {@link #sendResourcePack(String, byte[])}. To monitor the status
   * of the sent resource pack, subscribe to {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   */
  void sendResourcePack(String url);

  /**
   * Sends the specified resource pack from {@code url} to the user, if supported, and use the
   * 20-byte SHA-1 hash as an unique identifier for this resource oack. To monitor the status of
   * the sent resource pack, subscribe to {@link PlayerResourcePackStatusEvent}.
   *
   * @param url the URL for the resource pack
   * @param hash the SHA-1 hash value for the resource pack
   */
  void sendResourcePack(String url, byte[] hash);

}
