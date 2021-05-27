/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired when a player has finished connecting to the proxy and we need to choose the first server
 * to connect to.
 */
public class PlayerChooseInitialServerEventImpl implements PlayerChooseInitialServerEvent {

  private final Player player;
  private @Nullable RegisteredServer initialServer;

  /**
   * Constructs a PlayerChooseInitialServerEvent.
   * @param player the player that was connected
   * @param initialServer the initial server selected, may be {@code null}
   */
  public PlayerChooseInitialServerEventImpl(Player player, @Nullable RegisteredServer initialServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.initialServer = initialServer;
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public @Nullable RegisteredServer initialServer() {
    return initialServer;
  }

  /**
   * Sets the new initial server.
   * @param server the initial server the player should connect to
   */
  @Override
  public void setInitialServer(@Nullable RegisteredServer server) {
    this.initialServer = server;
  }

  @Override
  public String toString() {
    return "PlayerChooseInitialServerEvent{"
        + "player=" + player
        + ", initialServer=" + initialServer
        + '}';
  }
}
