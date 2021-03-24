/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

/**
 * This event is fired by the proxy after plugins have been loaded but before the proxy starts
 * accepting connections.
 */
public final class ProxyInitializeEvent {

  @Override
  public String toString() {
    return "ProxyInitializeEvent";
  }
}
