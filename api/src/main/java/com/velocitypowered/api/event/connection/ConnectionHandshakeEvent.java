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
  private final String extraDataInHandshake;

  /**
   * Creates a new event.
   *
   * @param connection the inbound connection
   * @param intent the intent of the handshake
   * @param extraDataInHandshake the extra data in the handshake
   */
  public ConnectionHandshakeEvent(InboundConnection connection, HandshakeIntent intent, String extraDataInHandshake) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.intent = Preconditions.checkNotNull(intent, "intent");
    this.extraDataInHandshake = Preconditions.checkNotNull(extraDataInHandshake, "extraDataInHandshake");
  }

  /**
   * This method is only retained to avoid breaking plugins
   * that have not yet updated their integration tests.
   *
   * @param connection the inbound connection
   * @deprecated use {@link #ConnectionHandshakeEvent(InboundConnection, HandshakeIntent, String)}
   */
  @Deprecated(forRemoval = true)
  public ConnectionHandshakeEvent(InboundConnection connection) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.intent = HandshakeIntent.LOGIN;
    this.extraDataInHandshake = "";
  }

  public InboundConnection getConnection() {
    return connection;
  }

  public HandshakeIntent getIntent() {
    return this.intent;
  }

  public String getExtraDataInHandshake() {
    return this.extraDataInHandshake;
  }

  @Override
  public String toString() {
    return "ConnectionHandshakeEvent{"
            + "connection=" + connection
            + ", intent=" + intent
            + '}';
  }
}
