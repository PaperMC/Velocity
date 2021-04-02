/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;

/**
 * This event is fired when a server list ping request is sent by a remote client.
 */
public final class ProxyPingEvent {

  private final InboundConnection connection;
  private ServerPing ping;

  public ProxyPingEvent(InboundConnection connection, ServerPing ping) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.ping = Preconditions.checkNotNull(ping, "ping");
  }

  public InboundConnection getConnection() {
    return connection;
  }

  public ServerPing getPing() {
    return ping;
  }

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
