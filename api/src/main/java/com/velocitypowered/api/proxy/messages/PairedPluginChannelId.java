/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import net.kyori.adventure.key.Key;

/**
 * Reperesents a legacy channel identifier (for Minecraft 1.12 and below). For modern 1.13 plugin
 * messages, please see {@link MinecraftPluginChannelId}. This class is immutable and safe for
 * multi-threaded use.
 */
public final class PairedPluginChannelId implements PluginChannelId {

  private final String legacyChannel;
  private final Key modernChannelKey;

  /**
   * Creates a new legacy channel identifier.
   *
   * @param legacyChannel the name for the legacy channel name
   * @param modernChannelKey the modern channel key to use
   */
  PairedPluginChannelId(String legacyChannel, Key modernChannelKey) {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(legacyChannel), "provided name is empty");
    this.legacyChannel = legacyChannel;
    this.modernChannelKey = Preconditions.checkNotNull(modernChannelKey, "modernChannelKey");
  }

  public String legacyChannel() {
    return legacyChannel;
  }

  public Key modernChannelKey() {
    return modernChannelKey;
  }

  @Override
  public String toString() {
    return legacyChannel + "/" + modernChannelKey.asString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PairedPluginChannelId that = (PairedPluginChannelId) o;

    if (!legacyChannel.equals(that.legacyChannel)) {
      return false;
    }
    return modernChannelKey.equals(that.modernChannelKey);
  }

  @Override
  public int hashCode() {
    int result = legacyChannel.hashCode();
    result = 31 * result + modernChannelKey.hashCode();
    return result;
  }
}
