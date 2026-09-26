/*
 * Copyright (C) 2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import org.jspecify.annotations.NonNull;

/**
 * Fired when a {@link Player} sends the <code>minecraft:custom_click_action</code> plugin message. Velocity will
 * not wait on the result of this event.
 *
 * @param player  the {@link Player} of the send custom click action
 * @param id      the send action id
 * @param payload the send action payload
 *
 * @since 4.2.1
 * @sinceMinecraft 1.21.6
 */
public record PlayerCustomClickEvent(@NonNull Player player, @NonNull Key id, @NonNull BinaryTag payload) {

  /**
   * Constructs a new PlayerCustomClickEvent.
   */
  public PlayerCustomClickEvent {
    Preconditions.checkNotNull(player);
    Preconditions.checkNotNull(id);
    Preconditions.checkNotNull(payload);
  }

  @Override
  public @NonNull String toString() {
    return "PlayerCustomClickEvent{"
        + "player=" + player
        + ", id='" + id + "'"
        + ", payload='" + payload + "'"
        + "}";
  }
}

