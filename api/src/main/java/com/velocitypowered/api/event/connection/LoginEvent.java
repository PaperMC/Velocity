/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired once the player has been authenticated, but before they connect to a server.
 * Velocity will wait for this event to finish firing before proceeding with the rest of the login
 * process, but you should try to limit the work done in any event that fires during the login
 * process.
 */
@AwaitingEvent
public final class LoginEvent implements ResultedEvent<ResultedEvent.ComponentResult> {

  private final Player player;
  private final String serverIdHash;
  private ComponentResult result;

  /**
   * Constructs a new {@link LoginEvent}.
   *
   * @param player the player who has completed authentication
   * @param serverIdHash the server ID hash sent to Mojang for authentication,
   *                     or {@code null} if the connection is in offline-mode
   */
  public LoginEvent(Player player, @Nullable String serverIdHash) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.serverIdHash = serverIdHash;
    this.result = ComponentResult.allowed();
  }

  /**
   * Constructs a new {@link LoginEvent}.
   *
   * @param player the player who has completed authentication
   * @deprecated Use {@link #LoginEvent(Player, String)}.
   */
  @Deprecated(forRemoval = true)
  public LoginEvent(Player player) {
    this(player, null);
  }

  /**
   * Returns the player who has completed authentication.
   *
   * @return the authenticated player
   */
  public Player getPlayer() {
    return player;
  }

  /**
   * Returns the server ID hash that was sent to Mojang to authenticate the player.
   * If the connection was in offline-mode, this returns {@code null}.
   *
   * @return the server ID hash that was sent to Mojang to authenticate the player
   */
  public @Nullable String getServerIdHash() {
    return serverIdHash;
  }

  @Override
  public ComponentResult getResult() {
    return result;
  }

  @Override
  public void setResult(ComponentResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "LoginEvent{"
        + "player=" + player
        + ", result=" + result
        + '}';
  }
}
