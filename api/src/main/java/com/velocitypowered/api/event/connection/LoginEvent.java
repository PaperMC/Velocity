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

/**
 * This event is fired once the player has been authenticated, but before they connect to a server.
 * Velocity will wait for this event to finish firing before proceeding with the rest of the login
 * process, but you should try to limit the work done in any event that fires during the login
 * process.
 */
@AwaitingEvent
public final class LoginEvent implements ResultedEvent<ResultedEvent.ComponentResult> {

  private final Player player;
  private ComponentResult result;

  public LoginEvent(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.result = ComponentResult.allowed();
  }

  public Player getPlayer() {
    return player;
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
