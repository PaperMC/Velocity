/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network;

/**
 * Represents the ClientIntent of a client in the Handshake state.
 */
public enum HandshakeIntent {
  STATUS(1),
  LOGIN(2),
  TRANSFER(3);

  private final int id;

  HandshakeIntent(int id) {
    this.id = id;
  }

  public int id() {
    return this.id;
  }

  /**
   * Obtain the HandshakeIntent by ID.
   *
   * @param id the intent id
   * @return the HandshakeIntent desired
   */
  public static HandshakeIntent getById(int id) {
    return switch (id) {
      case 1 -> STATUS;
      case 2 -> LOGIN;
      case 3 -> TRANSFER;
      default -> null;
    };
  }
}
