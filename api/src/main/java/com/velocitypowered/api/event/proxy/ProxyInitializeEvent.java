/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.velocitypowered.api.event.annotation.AwaitingEvent;

/**
 * This event is fired by the proxy after plugins have been loaded but before the proxy starts
 * accepting connections. Velocity will wait for this event to finish firing before it begins to
 * accept new connections.
 */
@AwaitingEvent
public final class ProxyInitializeEvent {

  @Override
  public String toString() {
    return "ProxyInitializeEvent";
  }
}
