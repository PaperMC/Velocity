/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Provides a fluent interface to send a connection request to another server on the proxy. A
 * connection request is created using {@link Player#createConnectionRequest(RegisteredServer)}.
 */
public interface ConnectionRequestBuilder {

  /**
   * Returns the server that this connection request represents.
   *
   * @return the server this request will connect to
   */
  RegisteredServer getServer();

  /**
   * Initiates the connection to the remote server and emits a result on the {@link
   * CompletableFuture} after the user has logged on. No messages will be communicated to the
   * client: the user is responsible for all error handling.
   *
   * @return a {@link CompletableFuture} representing the status of this connection
   */
  CompletableFuture<Result> connect();

  /**
   * Initiates the connection to the remote server and emits a result on the {@link
   * CompletableFuture} after the user has logged on. Velocity's own built-in handling will be used
   * to provide errors to the client.
   *
   * @return a {@link CompletableFuture} representing the status of this connection
   */
  CompletableFuture<Boolean> connectWithIndication();

  /**
   * Initiates the connection to the remote server without waiting for a result. Velocity will use
   * generic error handling code to notify the user.
   */
  void fireAndForget();

  /**
   * Represents the result of a connection request.
   */
  interface Result {

    /**
     * Determines whether or not the connection request was successful.
     *
     * @return whether or not the request succeeded
     */
    default boolean isSuccessful() {
      return getStatus() == Status.SUCCESS;
    }

    /**
     * Returns the status associated with this result.
     *
     * @return the status for this result
     */
    Status getStatus();

    /**
     * Returns an (optional) textual reason for the failure to connect to the server.
     *
     * @return the reason why the user could not connect to the server
     */
    Optional<net.kyori.adventure.text.Component> getReasonComponent();

    /**
     * Returns the server we actually tried to connect to.
     *
     * @return the server we actually tried to connect to
     */
    RegisteredServer getAttemptedConnection();
  }

  /**
   * Represents the status of a connection request initiated by a {@link ConnectionRequestBuilder}.
   */
  enum Status {
    /**
     * The player was successfully connected to the server.
     */
    SUCCESS,
    /**
     * The player is already connected to this server.
     */
    ALREADY_CONNECTED,
    /**
     * The connection is already in progress.
     */
    CONNECTION_IN_PROGRESS,
    /**
     * A plugin has cancelled this connection.
     */
    CONNECTION_CANCELLED,
    /**
     * The server disconnected the user. A reason may be provided in the {@link Result} object.
     */
    SERVER_DISCONNECTED
  }

}
