/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import net.kyori.adventure.key.Key;

/**
 * Represents a channel identifier for use with plugin messaging.
 */
public interface PluginChannelId {

  /**
   * Wraps the specified Minecraft key so it can be used as a {@link PluginChannelId}.
   * If the client is connected using Minecraft 1.12.2 or earlier, use the key as the
   * channel name.
   *
   * @param key the key instance to wrap
   * @return a wrapped plugin channel ID
   */
  static KeyedPluginChannelId wrap(Key key) {
    return new KeyedPluginChannelId(key);
  }

  /**
   * Wraps the specified Minecraft key so it can be used as a {@link PluginChannelId},
   * with the specified {@code legacyChannel} for clients connected using Minecraft 1.12.2
   * or earlier.
   *
   * @param legacyChannel the legacy channel name
   * @param modernChannelKey the key instance to wrap
   * @return a wrapped plugin channel ID
   */
  static PairedPluginChannelId withLegacy(String legacyChannel, Key modernChannelKey) {
    return new PairedPluginChannelId(legacyChannel, modernChannelKey);
  }
}
