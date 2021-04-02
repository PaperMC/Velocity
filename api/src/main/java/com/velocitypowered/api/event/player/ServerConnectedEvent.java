/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired once the player has successfully connected to the target server and the
 * connection to the previous server has been de-established.
 */
public final class ServerConnectedEvent {

  private final Player player;
  private final RegisteredServer server;
  private final @Nullable RegisteredServer previousServer;

  /**
   * Constructs a ServerConnectedEvent.
   * @param player the player that was connected
   * @param server the server the player was connected to
   * @param previousServer the server the player was previously connected to, null if none
   */
  public ServerConnectedEvent(Player player, RegisteredServer server,
      @Nullable RegisteredServer previousServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.server = Preconditions.checkNotNull(server, "server");
    this.previousServer = previousServer;
  }

  @Deprecated
  public ServerConnectedEvent(Player player, RegisteredServer server) {
    this(player, server, null);
  }

  public Player getPlayer() {
    return player;
  }

  public RegisteredServer getServer() {
    return server;
  }

  public Optional<RegisteredServer> getPreviousServer() {
    return Optional.ofNullable(previousServer);
  }

  @Override
  public String toString() {
    return "ServerConnectedEvent{"
        + "player=" + player
        + ", server=" + server
        + ", previousServer=" + previousServer
        + '}';
  }
}
