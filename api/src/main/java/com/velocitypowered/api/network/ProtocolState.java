/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network;

/**
 * Representation of the state of the protocol
 * in which a connection can be present.
 */
public enum ProtocolState {
  /**
   * Initial connection state.
   * <p>This status can be caused by a STATUS, LOGIN or TRANSFER intent.</p>
   * If the intent is LOGIN or TRANSFER, the connection will proceed, otherwise,
   * it will go to the STATUS state.
   */
  HANDSHAKE,
  /**
   * Ping status of a connection.
   * Connections with the STATUS HandshakeIntent will pass through this state
   * and be disconnected.
   */
  STATUS,
  /**
   * Authentication state of a connection, at this moment the player is authenticating
   * with the authentication servers (Mojang).
   */
  LOGIN,
  /**
   * Configuration State, at this point the player allows the server to send information
   * such as resource packs and plugin messages, at the same time the player
   * will send his client brand and the respective plugin messages if it is a modded client.
   *
   * @since Minecraft 1.20.5
   */
  CONFIGURATION,
  /**
   * In this state is where the whole game runs, the server is able to change
   * the player's state to Configuration as needed in versions 1.20.2 and higher.
   */
  PLAY;
}
