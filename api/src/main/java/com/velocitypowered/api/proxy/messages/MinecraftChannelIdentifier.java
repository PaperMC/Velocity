/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.messages;

import java.util.Objects;
import net.kyori.adventure.key.Key;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a Minecraft 1.13+ channel identifier. This class is immutable and safe for
 * multi-threaded use.
 */
final class MinecraftChannelIdentifier implements ChannelIdentifier {

  private final Key key;

  MinecraftChannelIdentifier(Key key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return this.key.asString() + " (modern)";
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MinecraftChannelIdentifier that = (MinecraftChannelIdentifier) o;
    return Objects.equals(key, that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String id() {
    return key.asString();
  }
}
