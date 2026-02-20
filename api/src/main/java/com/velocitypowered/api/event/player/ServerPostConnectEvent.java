/*
 * Copyright (C) 2020-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired after the player has connected to a server. The server the player is now connected to is
 * available in {@link Player#getCurrentServer()}. Velocity will not wait on this event to finish
 * firing.
 */
public class ServerPostConnectEvent {
  private final Player player;
  private final RegisteredServer previousServer;

  /**
   * Constructs a new {@link ServerPostConnectEvent}.
   *
   * @param player the player that connected to the server
   * @param previousServer the server the player was previously connected to, or {@code null}
   *                       if the player had not been connected to a server before
   */
  public ServerPostConnectEvent(Player player,
      @Nullable RegisteredServer previousServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.previousServer = previousServer;
  }

  /**
   * Returns the player that has completed the connection to the server.
   *
   * @return the player
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the previous server the player was connected to. This is {@code null} if they were not
   * connected to another server beforehand (for instance, if the player has just joined the proxy).
   *
   * @return the previous server the player was connected to
   */
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
