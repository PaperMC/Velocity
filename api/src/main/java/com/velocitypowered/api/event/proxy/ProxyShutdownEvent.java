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
