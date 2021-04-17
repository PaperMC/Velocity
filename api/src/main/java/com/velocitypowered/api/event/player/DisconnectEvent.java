/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.connection.Player;

public interface DisconnectEvent {

  Player getPlayer();

  LoginStatus getLoginStatus();

  public enum LoginStatus {

    SUCCESSFUL_LOGIN,
    CONFLICTING_LOGIN,
    CANCELLED_BY_USER,
    CANCELLED_BY_PROXY,
    CANCELLED_BY_USER_BEFORE_COMPLETE,
    PRE_SERVER_JOIN
  }
}
