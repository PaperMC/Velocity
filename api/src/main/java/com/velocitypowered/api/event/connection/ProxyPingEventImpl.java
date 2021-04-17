/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;

/**
 * This event is fired when a server list ping request is sent by a remote client.
 */
public final class ProxyPingEventImpl implements ProxyPingEvent {

  private final InboundConnection connection;
  private ServerPing ping;

  public ProxyPingEventImpl(InboundConnection connection, ServerPing ping) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.ping = Preconditions.checkNotNull(ping, "ping");
  }

  @Override
  public InboundConnection getConnection() {
    return connection;
  }

  @Override
  public ServerPing getPing() {
    return ping;
  }

  @Override
  public void setPing(ServerPing ping) {
    this.ping = Preconditions.checkNotNull(ping, "ping");
  }

  @Override
  public String toString() {
    return "ProxyPingEvent{"
        + "connection=" + connection
        + ", ping=" + ping
        + '}';
  }
}
