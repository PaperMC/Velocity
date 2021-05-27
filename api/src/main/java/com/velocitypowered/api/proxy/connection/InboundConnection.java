/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.connection;

import com.velocitypowered.api.network.ProtocolVersion;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents an incoming connection to the proxy.
 */
public interface InboundConnection {

  /**
   * Returns the player's remote address.
   *
   * @return the player's remote address
   */
  @Nullable SocketAddress remoteAddress();

  /**
   * Returns the hostname that the user entered into the client, if applicable.
   *
   * @return the hostname from the client
   */
  @Nullable InetSocketAddress connectedHostname();

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
  ProtocolVersion protocolVersion();
}
