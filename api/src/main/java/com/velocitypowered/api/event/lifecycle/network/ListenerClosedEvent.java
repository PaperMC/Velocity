/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.lifecycle.network;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ListenerType;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * This event is fired by the proxy before the proxy stops accepting connections.
 */
public final class ListenerClosedEvent {

  private final SocketAddress address;
  private final ListenerType listenerType;

  public ListenerClosedEvent(SocketAddress address, ListenerType listenerType) {
    this.address = Preconditions.checkNotNull(address, "address");
    this.listenerType = Preconditions.checkNotNull(listenerType, "listenerType");
  }

  public SocketAddress getAddress() {
    return address;
  }

  public ListenerType getListenerType() {
    return listenerType;
  }

  @Override
  public String toString() {
    return "ListenerCloseEvent{"
        + "address=" + address
        + ", listenerType=" + listenerType
        + '}';
  }
}