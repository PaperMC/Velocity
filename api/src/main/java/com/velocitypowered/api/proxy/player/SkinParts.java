/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents what, if any, extended parts of the skin this player has.
 */
public final class SkinParts {

  private final byte bitmask;

  public SkinParts(byte skinBitmask) {
    this.bitmask = skinBitmask;
  }

  public boolean hasCape() {
    return (bitmask & 1) == 1;
  }

  public boolean hasJacket() {
    return ((bitmask >> 1) & 1) == 1;
  }

  public boolean hasLeftSleeve() {
    return ((bitmask >> 2) & 1) == 1;
  }

  public boolean hasRightSleeve() {
    return ((bitmask >> 3) & 1) == 1;
  }

  public boolean hasLeftPants() {
    return ((bitmask >> 4) & 1) == 1;
  }

  public boolean hasRightPants() {
    return ((bitmask >> 5) & 1) == 1;
  }

  public boolean hasHat() {
    return ((bitmask >> 6) & 1) == 1;
  }

  @Override
  public boolean equals(@Nullable final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final SkinParts skinParts = (SkinParts) o;
    return bitmask == skinParts.bitmask;
  }

  @Override
  public int hashCode() {
    return Objects.hash(bitmask);
  }
}
