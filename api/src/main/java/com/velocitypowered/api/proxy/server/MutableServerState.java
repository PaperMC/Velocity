/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.velocitypowered.api.proxy.Player;

/**
 * A mutable view on a server's state.
 *
 * <p>This interface's methods may be called from different threads. As such, <strong>it is important to consider
 * thread-safety in implementations.</strong>
 */
public interface MutableServerState extends ServerState {
  /**
   * Called when a player has switched to the {@link RegisteredServer} associated with this state.
   *
   * @param player the player
   */
  void addPlayer(Player player);

  /**
   * Called when a player has disconnected from {@link RegisteredServer} associated with this state.
   *
   * @param player the player
   */
  void removePlayer(Player player);
}
