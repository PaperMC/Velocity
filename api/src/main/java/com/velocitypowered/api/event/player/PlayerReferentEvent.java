/*
 * Copyright (C) 2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.Player;

/**
 * Defines any event that refers to a player.
 */
public interface PlayerReferentEvent {

  Player player();
}
