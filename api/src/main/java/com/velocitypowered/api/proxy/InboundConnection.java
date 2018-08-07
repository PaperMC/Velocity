package com.velocitypowered.api.proxy;

import java.net.InetSocketAddress;

/**
 * Represents a connection to the proxy. There is no guarantee that the connection has been fully initialized.
 */
public interface InboundConnection {
    /**
     * Returns the player's IP address.
     * @return the player's IP
     */
    InetSocketAddress getRemoteAddress();

    /**
     * Determine whether or not the player remains online.
     * @return whether or not the player active
     */
    boolean isActive();

    /**
     * Returns the current protocol version this connection uses.
     * @return the protocol version the connection uses
     */
    int getProtocolVersion();
}
