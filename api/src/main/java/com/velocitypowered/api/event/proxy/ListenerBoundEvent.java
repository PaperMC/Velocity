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
 * This event is fired by the proxy after a listener starts accepting connections.
 */
public final class ListenerBoundEvent {

  private final InetSocketAddress address;
  private final boolean isQuery;

  public ListenerBoundEvent(InetSocketAddress address, boolean isQuery) {
    this.address = Preconditions.checkNotNull(address, "address");
    this.isQuery = isQuery;
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  public boolean isQuery() {
    return isQuery;
  }

  @Override
  public String toString() {
    return "ListenerBoundEvent";
  }
}
