package com.velocitypowered.api.event.proxy;

/**
 * This event is fired by the proxy after plugins have been loaded but before the proxy starts accepting connections.
 */
public final class ProxyInitializeEvent {
    @Override
    public String toString() {
        return "ProxyInitializeEvent";
    }
}
