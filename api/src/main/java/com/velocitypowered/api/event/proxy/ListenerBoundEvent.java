package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;

/**
 * This event is fired by the proxy after a listener starts accepting connections.
 */
public final class ListenerBoundEvent {

  private final InetSocketAddress address;

  public ListenerBoundEvent(InetSocketAddress address) {
    this.address = Preconditions.checkNotNull(address, "address");
  }

  public InetSocketAddress getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "ProxyBoundEvent";
  }
}
