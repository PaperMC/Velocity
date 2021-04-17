/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import com.google.common.base.Preconditions;
import net.kyori.adventure.key.Key;

/**
 * Represents a modern namespaced channel identifier.
 */
public final class KeyedPluginChannelId implements PluginChannelId {

  private final Key key;

  KeyedPluginChannelId(Key key) {
    this.key = Preconditions.checkNotNull(key, "key");
  }

  public Key key() {
    return key;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    KeyedPluginChannelId that = (KeyedPluginChannelId) o;

    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return key.asString();
  }
}
