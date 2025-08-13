/*
 * Copyright (C) 2018-2025 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.api.event.proxy;

import com.velocitypowered.api.event.annotation.AwaitingEvent;

/**
 * This event is fired by the proxy after it has stopped accepting new connections,
 * but before players are disconnected.
 * <p>
 * This is the last point at which you can interact with currently connected players,
 * for example to transfer them to another proxy or perform other cleanup tasks.
 * <p>
 * Velocity will wait for all event listeners to complete before disconnecting players,
 * but note that the event will time out after a certain period (currently 10 seconds)
 * to prevent shutdown from hanging indefinitely.
 */
@AwaitingEvent
public final class ProxyPreShutdownEvent {

  @Override
  public String toString() {
    return "ProxyPreShutdownEvent";
  }
}
