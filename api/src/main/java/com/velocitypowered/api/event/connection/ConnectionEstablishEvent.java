/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Event called when a connection is initially established with the proxy.
 * Velocity will wait for this event to fire before continuing with the connection.
 */
@AwaitingEvent
public class ConnectionEstablishEvent implements ResultedEvent<ResultedEvent.GenericResult> {
  private final InboundConnection connection;
  private final Intention intention;
  private GenericResult result = GenericResult.allowed();

  public ConnectionEstablishEvent(
          final @NonNull InboundConnection connection,
          final @Nullable Intention intention
  ) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.intention = intention;
  }

  /**
   * The intention of the connection.
   */
  public enum Intention {
    /**
     * The user intends to ping the server to fetch the status.
     */
    STATUS,
    /**
     * The user intends to log in to the server.
     */
    LOGIN,
  }

  /**
   * Returns the inbound connection that is being established.
   *
   * @return the connection
   */
  public @NonNull InboundConnection getConnection() {
    return this.connection;
  }

  /**
   * Returns the intention for which the connection is being established, if known.
   *
   * @return the intention
   */
  public @Nullable Intention getIntention() {
    return this.intention;
  }

  @Override
  public GenericResult getResult() {
    return this.result;
  }

  @Override
  public void setResult(final @NonNull GenericResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }
}
