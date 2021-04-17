/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;

/**
 * Fired when a player has finished connecting to the proxy and we need to choose the first server
 * to connect to.
 */
public interface PlayerChooseInitialServerEvent {

  Player getPlayer();

  Optional<RegisteredServer> getInitialServer();

  /**
   * Sets the new initial server.
   *
   * @param server the initial server the player should connect to
   */
  void setInitialServer(RegisteredServer server);
}
