/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired once the player has been fully initialized and is about to connect to their
 * first server.
 */
public interface PostLoginEvent extends Event {

  Player player();
}
