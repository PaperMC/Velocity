/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

/**
 * Describes whether the original client-submitted chat packet carried a message signature.
 *
 * @since 3.6.0
 */
public enum PlayerChatSignedState {
  /**
   * The protocol generation does not carry modern signed-chat metadata.
   */
  LEGACY,
  /**
   * The protocol generation supports signed chat, but this packet did not contain a signature.
   */
  UNSIGNED,
  /**
   * The packet contained a signature, but Velocity does not have a complete public
   * representation of the signing metadata.
   */
  SIGNED,
  /**
   * The packet contained a keyed-chat signature and key metadata.
   */
  KEYED_SIGNED,
  /**
   * The packet contained a session-chat signature and Velocity had matching session metadata.
   */
  SESSION_SIGNED
}
