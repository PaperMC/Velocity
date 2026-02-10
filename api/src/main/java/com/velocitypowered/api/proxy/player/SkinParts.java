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

  /**
   * Constructs a new SkinParts object with the provided bitmask.
   *
   * @param skinBitmask the bitmask representing which skin parts are enabled
   */
  public SkinParts(byte skinBitmask) {
    this.bitmask = skinBitmask;
  }

  /**
   * Returns whether the player has a cape enabled.
   *
   * @return true if the cape is enabled, false otherwise
   */
  public boolean hasCape() {
    return (bitmask & 1) == 1;
  }

  /**
   * Returns whether the player has a jacket enabled.
   *
   * @return true if the jacket is enabled, false otherwise
   */
  public boolean hasJacket() {
    return ((bitmask >> 1) & 1) == 1;
  }

  /**
   * Returns whether the player has a left sleeve enabled.
   *
   * @return true if the left sleeve is enabled, false otherwise
   */
  public boolean hasLeftSleeve() {
    return ((bitmask >> 2) & 1) == 1;
  }

  /**
   * Returns whether the player has a right sleeve enabled.
   *
   * @return true if the right sleeve is enabled, false otherwise
   */
  public boolean hasRightSleeve() {
    return ((bitmask >> 3) & 1) == 1;
  }

  /**
   * Returns whether the player has their left pants enabled.
   *
   * @return true if the left pants are enabled, false otherwise
   */
  public boolean hasLeftPants() {
    return ((bitmask >> 4) & 1) == 1;
  }

  /**
   * Returns whether the player has their right pants enabled.
   *
   * @return true if the right pants are enabled, false otherwise
   */
  public boolean hasRightPants() {
    return ((bitmask >> 5) & 1) == 1;
  }

  /**
   * Returns whether the player has a hat enabled.
   *
   * @return true if the hat is enabled, false otherwise
   */
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
