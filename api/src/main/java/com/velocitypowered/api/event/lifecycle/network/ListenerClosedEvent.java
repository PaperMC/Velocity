/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.lifecycle.network;

import com.velocitypowered.api.network.ListenerType;
import java.net.SocketAddress;

/**
 * This event is fired by the proxy before the proxy stops accepting connections.
 */
public interface ListenerClosedEvent {

  SocketAddress getAddress();

  ListenerType getListenerType();
}
