package com.velocitypowered.api.proxy.connection;

import com.velocitypowered.api.network.ProtocolVersion;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Optional;

/**
 * Represents an incoming connection to the proxy.
 */
public interface InboundConnection {

  /**
   * Returns the player's remote address.
   *
   * @return the player's remote address
   */
  SocketAddress getRemoteAddress();

  /**
   * Returns the hostname that the user entered into the client, if applicable.
   *
   * @return the hostname from the client
   */
  Optional<InetSocketAddress> getVirtualHost();

  /**
   * Determine whether or not the player remains online.
   *
   * @return whether or not the player active
   */
  boolean isActive();

  /**
   * Returns the current protocol version this connection uses.
   *
   * @return the protocol version the connection uses
   */
  ProtocolVersion getProtocolVersion();
}
