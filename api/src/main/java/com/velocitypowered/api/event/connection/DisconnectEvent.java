/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired when a player disconnects from the proxy. This operation can take place
 * when the player disconnects due to normal network activity or when the proxy shuts down.
 * Operations on the provided player, aside from basic data retrieval operations, may behave in
 * undefined ways.
 *
 * <p>
 *   Velocity typically fires this event asynchronously and does not wait for a response. However,
 *   it will wait for all {@link DisconnectEvent}s for every player on the proxy to fire
 *   successfully before the proxy shuts down. This event is the sole exception to the
 *   {@link AwaitingEvent} contract.
 * </p>
 */
@AwaitingEvent
public final class DisconnectEvent {

  private final Player player;
  private final LoginStatus loginStatus;

  public DisconnectEvent(Player player, LoginStatus loginStatus) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.loginStatus = Preconditions.checkNotNull(loginStatus, "loginStatus");
  }

  public Player getPlayer() {
    return player;
  }

  public LoginStatus getLoginStatus() {
    return loginStatus;
  }

  @Override
  public String toString() {
    return "DisconnectEvent{"
        + "player=" + player + ", "
        + "loginStatus=" + loginStatus
        + '}';
  }

  /**
   * The status of the connection when the player disconnected.
   */
  public enum LoginStatus {

    SUCCESSFUL_LOGIN,
    CONFLICTING_LOGIN,
    CANCELLED_BY_USER,
    CANCELLED_BY_PROXY,
    CANCELLED_BY_USER_BEFORE_COMPLETE,
    PRE_SERVER_JOIN
  }
}
