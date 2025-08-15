/*
 * Copyright (C) 2018-2025 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.google.common.annotations.Beta;
import com.velocitypowered.api.event.annotation.AwaitingEvent;

/**
 * This event is fired by the proxy after it has stopped accepting new connections,
 * but before players are disconnected.
 * This is the last point at which you can interact with currently connected players,
 * for example to transfer them to another proxy or perform other cleanup tasks.
 *
 * <p><b>Note:</b> Velocity will wait for all event listeners to complete before disconnecting players,
 * but note that the event will time out after 10 seconds
 * to prevent shutdown from hanging indefinitely.</p>
 */
@Beta
@AwaitingEvent
public final class ProxyPreShutdownEvent {

  @Override
  public String toString() {
    return "ProxyPreShutdownEvent";
  }
}
