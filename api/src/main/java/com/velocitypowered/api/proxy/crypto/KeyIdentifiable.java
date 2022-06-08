/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.crypto;

/**
 * Identifies a type with a public RSA signature.
 */
public interface KeyIdentifiable {

  /**
   * Returns the timed identified key of the object context.
   * <p>Only available in 1.19 and newer</p>
   * @return the key or null if not available
   */
  IdentifiedKey getIdentifiedKey();
}
