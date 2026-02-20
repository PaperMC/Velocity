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

  /**
   * Constructs a new {@code ProxyPingEvent}.
   *
   * @param connection the incoming connection requesting server info
   * @param ping the server ping response to send
   */
  public ProxyPingEvent(final InboundConnection connection, final ServerPing ping) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.ping = Preconditions.checkNotNull(ping, "ping");
  }

  /**
   * Obtain the connection to which the corresponding ServerPing will be sent.
   *
   * @return the connection that has sent the ServerPing request
   */
  public InboundConnection getConnection() {
    return this.connection;
  }

  /**
   * Get the ServerPing to send to the connection.
   *
   * @return the ServerPing to send
   */
  public ServerPing getPing() {
    return this.ping;
  }

  /**
   * Sets the ServerPing to send to the connection.
   *
   * @param ping sets the ServerPing to send
   */
  public void setPing(final @NotNull ServerPing ping) {
    this.ping = Preconditions.checkNotNull(ping, "ping");
  }

  /**
   * Gets whether to avoid sending a ping response to the connection.
   *
   * @return if a ping response to the connection will be avoided
   * @apiNote For the ProxyPingEvent executed to obtain the MOTD for the ServerData
   *     sent to players of versions higher than 1.19.1,
   *     the cancellation of this event will have no effect.
   */
  @Override
  public GenericResult getResult() {
    return this.result;
  }

  /**
   * Sets whether to avoid sending a ping response to the connection.
   * This will automatically close the connection.
   *
   * @param result if a ping response to the connection will be avoided
   * @apiNote For the ProxyPingEvent executed to obtain the MOTD for the ServerData
   *     sent to players of versions higher than 1.19.1,
   *     the cancellation of this event will have no effect.
   */
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
