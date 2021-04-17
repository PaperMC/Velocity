/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired after the player has connected to a server. The server the player is now connected to is
 * available in {@link Player#getCurrentServer()}.
 */
public interface ServerPostConnectEvent {

  Player getPlayer();

  @Nullable RegisteredServer getPreviousServer();
}
