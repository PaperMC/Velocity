/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import net.kyori.adventure.key.Key;

/**
 * Represents a channel identifier for use with plugin messaging.
 */
public interface ChannelIdentifier {

  /**
   * Returns the textual representation of this identifier.
   *
   * @return the textual representation of the identifier
   */
  String id();

  /**
   * Returns a channel identifier to identify a channel for Minecraft clients older than Minecraft
   * 1.13.
   *
   * @param name the channel name to use
   * @return the channel identifier with the given name
   */
  static ChannelIdentifier legacy(String name) {
    return new LegacyChannelIdentifier(name);
  }


  /**
   * Returns a channel identifier to identify a channel for Minecraft clients newer or equal to
   * Minecraft 1.13. This uses a Minecraft resource code of the form {@code namespace:name}.
   *
   * @param key the channel name to use
   * @return the channel identifier with the given name
   */
  static ChannelIdentifier ofKey(Key key) {
    return new MinecraftChannelIdentifier(key);
  }
}
