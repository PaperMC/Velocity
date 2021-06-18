/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.player;

import net.kyori.adventure.identity.Identified;
import net.kyori.adventure.identity.Identity;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Indicates an identity for a given player. This is a marker interface.
 */
public interface PlayerIdentity extends Identified, Identity {

  /**
   * Returns a "friendly name" to identity the player as.
   *
   * @return a friendly name to use for the player
   */
  String name();

  @Override
  default @NonNull Identity identity() {
    return this;
  }
}
