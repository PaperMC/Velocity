/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.HandshakeIntent;
import com.velocitypowered.api.proxy.InboundConnection;

/**
 * This event is fired when a handshake is established between a client and the proxy.
 * Velocity will fire this event asynchronously and will not wait for it to complete before
 * handling the connection.
 */
public final class ConnectionHandshakeEvent {

  private final InboundConnection connection;
  private final HandshakeIntent intent;

  /**
   * Constructs a new {@link ConnectionHandshakeEvent}.
   *
   * @param connection the inbound connection from the client
   * @param intent the intent of the handshake (e.g., login or status)
   */
  public ConnectionHandshakeEvent(InboundConnection connection, HandshakeIntent intent) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.intent = Preconditions.checkNotNull(intent, "intent");
  }

  /**
   * This method is only retained to avoid breaking plugins
   * that have not yet updated their integration tests.
   *
   * @param connection the inbound connection
   * @deprecated use {@link #ConnectionHandshakeEvent(InboundConnection, HandshakeIntent)}
   */
  @Deprecated(forRemoval = true)
  public ConnectionHandshakeEvent(InboundConnection connection) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.intent = HandshakeIntent.LOGIN;
  }

  /**
   * Returns the inbound connection associated with this handshake.
   *
   * @return the connection
   */
  public InboundConnection getConnection() {
    return connection;
  }

  /**
   * Returns the {@link HandshakeIntent} associated with this connection handshake.
   *
   * @return the intent of the handshake
   */
  public HandshakeIntent getIntent() {
    return this.intent;
  }

  @Override
  public String toString() {
    return "ConnectionHandshakeEvent{"
        + "connection=" + connection
        + ", intent=" + intent
        + '}';
  }
}
