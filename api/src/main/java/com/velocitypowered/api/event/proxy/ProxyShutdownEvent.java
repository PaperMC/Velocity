/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

/**
 * This event is fired by the proxy after the proxy has stopped accepting connections but before the
 * proxy process exits.
 */
public final class ProxyShutdownEvent {

  @Override
  public String toString() {
    return "ProxyShutdownEvent";
  }
}
