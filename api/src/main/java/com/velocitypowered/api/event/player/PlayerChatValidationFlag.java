/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

/**
 * Validation facts Velocity knows about the original player-chat packet.
 *
 * <p>These flags are deliberately granular: a signed packet is not automatically a validated
 * packet. A flag is present only when Velocity had the data and performed or observed that exact
 * condition in the current protocol path.</p>
 *
 * @since 3.6.0
 */
public enum PlayerChatValidationFlag {
  /**
   * The packet did not contain a message signature.
   */
  UNSIGNED,
  /**
   * The packet contained signature bytes.
   */
  SIGNATURE_PRESENT,
  /**
   * The signed packet had the protocol fields Velocity needs to describe the signature.
   */
  STRUCTURALLY_COMPLETE,
  /**
   * Velocity had public-key metadata associated with the signing identity.
   */
  KEY_AVAILABLE,
  /**
   * Velocity had session metadata and it belonged to the sending player's UUID.
   */
  SESSION_MATCHED,
  /**
   * The packet included last-seen or message-chain state.
   */
  CHAIN_STATE_AVAILABLE,
  /**
   * Velocity verified the message signature against the signing key.
   */
  SIGNATURE_VALIDATED,
  /**
   * Velocity validated the last-seen or message-chain state.
   */
  CHAIN_VALIDATED,
  /**
   * Validation could not be completed or is not performed by the current Velocity path.
   */
  VALIDATION_UNAVAILABLE
}
