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

/**
 * This event is fired before the player connects to a server.
 */
public final class ServerPreConnectEventImpl implements ServerPreConnectEvent {

  private final Player player;
  private final RegisteredServer originalServer;
  private ServerResult result;

  /**
   * Creates the ServerPreConnectEvent.
   * @param player the player who is connecting to a server
   * @param originalServer the server the player was trying to connect to
   */
  public ServerPreConnectEventImpl(Player player, RegisteredServer originalServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.originalServer = Preconditions.checkNotNull(originalServer, "originalServer");
    this.result = ServerResult.allowed(originalServer);
  }

  /**
   * Returns the player connecting to the server.
   * @return the player connecting to the server
   */
  @Override
  public Player player() {
    return player;
  }

  @Override
  public ServerResult result() {
    return result;
  }

  @Override
  public void setResult(ServerResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  /**
   * Returns the server that the player originally tried to connect to. To get the server the
   * player will connect to, see the {@link ServerResult} of this event. To get the server the
   * player is currently on when this event is fired, use {@link Player#connectedServer()}.
   * @return the server that the player originally tried to connect to
   */
  @Override
  public RegisteredServer originalTarget() {
    return originalServer;
  }

  @Override
  public String toString() {
    return "ServerPreConnectEvent{"
        + "player=" + player
        + ", originalServer=" + originalServer
        + ", result=" + result
        + '}';
  }

}
