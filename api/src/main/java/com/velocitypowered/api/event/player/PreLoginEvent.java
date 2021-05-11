/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import net.kyori.adventure.text.Component;

/**
 * This event is fired when a player has initiated a connection with the proxy but before the proxy
 * authenticates the player with Mojang or before the player's proxy connection is fully established
 * (for offline mode).
 */
public interface PreLoginEvent extends ResultedEvent<ComponentResult> {

  InboundConnection connection();

  String username();

  boolean onlineMode();

  void setOnlineMode(boolean onlineMode);

  default void allow() {
    setResult(ComponentResult.allowed());
  }

  default void reject(Component component) {
    setResult(ComponentResult.denied(component));
  }
}
