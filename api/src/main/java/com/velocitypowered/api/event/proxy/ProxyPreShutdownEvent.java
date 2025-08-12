/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.velocitypowered.api.event.annotation.AwaitingEvent;

/**
 * This event is fired by the proxy after it has stopped accepting new connections,
 * but before players are disconnected.
 * This is the last point at which you can interact with currently connected players,
 * for example to transfer them to another proxy or perform other cleanup tasks.
 * Velocity will wait for all event listeners to complete before disconnecting players.
 */
@AwaitingEvent
public final class ProxyPreShutdownEvent {

  @Override
  public String toString() {
    return "ProxyPreShutdownEvent";
  }
}
