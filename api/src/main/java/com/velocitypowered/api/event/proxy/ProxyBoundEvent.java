package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

import java.net.InetSocketAddress;

/**
 * This event is fired by the proxy after the proxy starts accepting connections.
 */
public final class ProxyBoundEvent {

  private final InetSocketAddress address;

  public ProxyBoundEvent(@NotNull InetSocketAddress address) {
    this.address = Preconditions.checkNotNull(address, "address");
  }

  @NotNull
  public InetSocketAddress getAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "ProxyBoundEvent";
  }
}
