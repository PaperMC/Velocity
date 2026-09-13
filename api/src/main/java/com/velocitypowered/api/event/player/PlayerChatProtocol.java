/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

/**
 * Identifies the Minecraft player-chat protocol generation used by the original message.
 *
 * @since 3.6.0
 */
public enum PlayerChatProtocol {
  /**
   * Chat before Minecraft 1.19. These packets contain only plaintext chat input.
   */
  LEGACY_UNSIGNED,
  /**
   * Minecraft 1.19 through 1.19.2 keyed chat. Signed messages use the player's login/session
   * identified key directly.
   */
  KEYED_CHAT,
  /**
   * Minecraft 1.19.3 and newer session chat. Signed messages use a chat session announced
   * through player-info {@code INITIALIZE_CHAT} data.
   */
  SESSION_CHAT
}
