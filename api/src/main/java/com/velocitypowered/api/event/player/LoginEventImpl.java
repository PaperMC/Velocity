/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired once the player has been authenticated but before they connect to a server on
 * the proxy.
 */
public final class LoginEventImpl implements LoginEvent {

  private final Player player;
  private ComponentResult result;

  public LoginEventImpl(Player player) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.result = ComponentResult.allowed();
  }

  @Override
  public Player player() {
    return player;
  }

  @Override
  public ComponentResult result() {
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
