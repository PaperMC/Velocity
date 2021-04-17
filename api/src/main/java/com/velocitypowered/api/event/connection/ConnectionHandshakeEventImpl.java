/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.InboundConnection;

/**
 * This event is fired when a handshake is established between a client and the proxy.
 */
public final class ConnectionHandshakeEventImpl implements ConnectionHandshakeEvent {

  private final InboundConnection connection;

  public ConnectionHandshakeEventImpl(InboundConnection connection) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
  }

  public InboundConnection getConnection() {
    return connection;
  }

  @Override
  public String toString() {
    return "ConnectionHandshakeEvent{"
        + "connection=" + connection
        + '}';
  }
}