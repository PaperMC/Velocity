/*
 * Copyright (C) 2019-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Fired when a player has finished the login process, and we need to choose the first server
 * to connect to. Velocity will wait on this event to finish firing before initiating the connection
 * but you should try to limit the work done in this event. Failures will be handled by
 * {@link KickedFromServerEvent} as normal.
 */
@AwaitingEvent
public class PlayerChooseInitialServerEvent {

  private final Player player;
  private @Nullable RegisteredServer initialServer;

  /**
   * Constructs a PlayerChooseInitialServerEvent.
   *
   * @param player the player that was connected
   * @param initialServer the initial server selected, may be {@code null}
   */
  public PlayerChooseInitialServerEvent(Player player, @Nullable RegisteredServer initialServer) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.initialServer = initialServer;
  }

  /**
   * Gets the player who is choosing the initial server.
   *
   * @return the connected player
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Gets the initial server the player will connect to.
   *
   * @return an {@link Optional} containing the selected server, or empty if none was set
   */
  public Optional<RegisteredServer> getInitialServer() {
    return Optional.ofNullable(initialServer);
  }

  /**
   * Sets the new initial server.
   *
   * @param server the initial server the player should connect to
   */
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
