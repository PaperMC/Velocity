/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.CANCELLED_BY_PROXY;
import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.CONFLICTING_LOGIN;
import static com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;

/**
 * This event is fired when a player disconnects from the proxy. Operations on the provided player,
 * aside from basic data retrieval operations, may behave in undefined ways.
 */
public final class DisconnectEvent {

  private final Player player;
  private final LoginStatus loginStatus;

  @Deprecated
  public DisconnectEvent(Player player) {
    this(player, false);
  }

  @Deprecated
  public DisconnectEvent(Player player,
      boolean disconnectedDuringLogin) {
    this(player, disconnectedDuringLogin ? CANCELLED_BY_PROXY : SUCCESSFUL_LOGIN);
  }

  public DisconnectEvent(Player player, LoginStatus loginStatus) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.loginStatus = Preconditions.checkNotNull(loginStatus, "loginStatus");
  }

  public Player getPlayer() {
    return player;
  }

  @Deprecated
  public boolean disconnectedDuringLogin() {
    return this.loginStatus == CANCELLED_BY_PROXY || this.loginStatus == CONFLICTING_LOGIN;
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

  public enum LoginStatus {

    SUCCESSFUL_LOGIN,
    CONFLICTING_LOGIN,
    CANCELLED_BY_USER,
    CANCELLED_BY_PROXY,
    CANCELLED_BY_USER_BEFORE_COMPLETE,
    PRE_SERVER_JOIN
  }
}
