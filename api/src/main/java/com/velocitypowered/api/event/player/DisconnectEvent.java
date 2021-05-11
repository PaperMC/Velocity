/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.connection.Player;

/**
 * This event is fired when a player disconnects from the proxy. Operations on the provided player,
 * aside from basic data retrieval operations, may behave in undefined ways.
 */
public interface DisconnectEvent {

  Player player();

  LoginStatus loginStatus();

  public enum LoginStatus {

    SUCCESSFUL_LOGIN,
    CONFLICTING_LOGIN,
    CANCELLED_BY_USER,
    CANCELLED_BY_PROXY,
    CANCELLED_BY_USER_BEFORE_COMPLETE,
    PRE_SERVER_JOIN
  }
}
