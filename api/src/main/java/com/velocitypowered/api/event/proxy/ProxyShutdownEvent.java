/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.velocitypowered.api.event.annotation.AwaitingEvent;

/**
 * This event is fired by the proxy after the proxy has stopped accepting connections but before the
 * proxy process exits. Velocity will wait for this event to finish firing before it exits.
 */
@AwaitingEvent
public final class ProxyShutdownEvent {

  /**
   * Creates a new {@code ProxyShutdownEvent}.
   */
  public ProxyShutdownEvent() {
  }

  @Override
  public String toString() {
    return "ProxyShutdownEvent";
  }
}
