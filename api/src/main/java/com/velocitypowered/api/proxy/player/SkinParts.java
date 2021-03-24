/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

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
}
