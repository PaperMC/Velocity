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
  /**
   * Indicates that the client is performing a status request (e.g., server list ping).
   */
  STATUS(1),
  /**
   * Indicates that the client intends to log in to the server.
   */
  LOGIN(2),
  /**
   * Indicates that the client is initiating a transfer (e.g., Velocity-native forwarding).
   */
  TRANSFER(3);

  /**
   * The numeric ID associated with this handshake intent.
   */
  private final int id;

  HandshakeIntent(int id) {
    this.id = id;
  }

  /**
   * Returns the numeric ID associated with this handshake intent.
   *
   * @return the handshake intent ID
   */
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
