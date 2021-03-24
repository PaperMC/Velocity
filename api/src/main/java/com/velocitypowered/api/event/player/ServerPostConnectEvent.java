/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired after the player has connected to a server. The server the player is now connected to is
 * available in {@link Player#getCurrentServer()}.
 */
@Beta
public class ServerPostConnectEvent {
  private final Player player;
  private final RegisteredServer previousServer;

  public ServerPostConnectEvent(Player player,
      @Nullable RegisteredServer previousServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.previousServer = previousServer;
  }

  public Player getPlayer() {
    return player;
  }

  public @Nullable RegisteredServer getPreviousServer() {
    return previousServer;
  }

  @Override
  public String toString() {
    return "ServerPostConnectEvent{"
        + "player=" + player
        + ", previousServer=" + previousServer
        + '}';
  }
}
