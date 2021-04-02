/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;

/**
 * This event is fired by the proxy before the proxy stops accepting connections.
 */
public final class ListenerCloseEvent {

  private final InetSocketAddress address;

  public ListenerCloseEvent(InetSocketAddress address) {
    this.address = Preconditions.checkNotNull(address, "address");
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "ListenerCloseEvent";
  }
}
