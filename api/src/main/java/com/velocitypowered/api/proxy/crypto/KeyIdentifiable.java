/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Identifies a type with a public RSA signature.
 */
public interface KeyIdentifiable {

  /**
   * Returns the timed identified key of the object context. This is only available if the client
   * is running Minecraft 1.19 or newer.
   *
   * @return the key or null if not available
   */
  @Nullable IdentifiedKey getIdentifiedKey();
}
