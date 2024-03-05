/*
 * Copyright (C) 2018-2022 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import org.jetbrains.annotations.NotNull;

/**
 * This event is fired when a request for server information is sent by a remote client, or when the
 * server sends the MOTD and favicon to the client after a successful login. Velocity will
 * wait on this event to finish firing before delivering the results to the remote client, but
 * you are urged to handle this event as quickly as possible when handling this event due to the
 * amount of ping packets a client can send.
 */
@AwaitingEvent
public final class ProxyPingEvent implements ResultedEvent<ResultedEvent.GenericResult> {

  private final InboundConnection connection;
  private ServerPing ping;
  private GenericResult result = GenericResult.allowed();

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
  public GenericResult getResult() {
    return this.result;
  }

  @Override
  public void setResult(final @NotNull GenericResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "ProxyPingEvent{"
        + "connection=" + connection
        + ", ping=" + ping
        + '}';
  }
}
