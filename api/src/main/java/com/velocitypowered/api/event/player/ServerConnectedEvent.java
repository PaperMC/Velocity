/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired once the player has successfully connected to the target server and the
 * connection to the previous server has been de-established.
 */
public interface ServerConnectedEvent extends Event {

  Player player();

  RegisteredServer target();

  @Nullable RegisteredServer previousServer();
}
