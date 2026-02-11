/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a server that has been registered with the proxy. The {@code Audience} associated with
 * a {@code RegisteredServer} represent all players on the server connected to this proxy and do not
 * interact with the server in any way.
 */
public interface RegisteredServer extends ChannelMessageSink, Audience {

  /**
   * Returns the {@link ServerInfo} for this server.
   *
   * @return the server info
   */
  ServerInfo getServerInfo();

  /**
   * Returns a list of all the players currently connected to this server on this proxy.
   *
   * @return the players on this proxy
   */
  Collection<Player> getPlayersConnected();

  /**
   * Attempts to ping the remote server and return the server list ping result.
   *
   * @return the server ping result from the server
   */
  CompletableFuture<ServerPing> ping();

  /**
   * Attempts to ping the remote server and return the server list ping result
   * according to the options provided.
   *
   * @param pingOptions the options provided for pinging the server
   * @return the server ping result from the server
   * @since 3.2.0
   */
  CompletableFuture<ServerPing> ping(PingOptions pingOptions);

  /**
   * Returns the current health status of this server.
   *
   * @return the health status
   * @since 3.5.0
   */
  @NonNull ServerHealthStatus getHealthStatus();

  /**
   * Returns detailed health information about this server.
   *
   * @return the health information
   * @since 3.5.0
   */
  @NonNull ServerHealthInfo getHealthInfo();

  /**
   * Returns whether this server is healthy and available for connections.
   *
   * @return {@code true} if the server is healthy
   * @since 3.5.0
   */
  default boolean isHealthy() {
    return getHealthStatus() == ServerHealthStatus.HEALTHY;
  }

  /**
   * Returns whether this server is available for player connections.
   * A server is available if its status is either HEALTHY or DEGRADED.
   *
   * @return {@code true} if the server is available for connections
   * @since 3.5.0
   */
  default boolean isAvailable() {
    return getHealthInfo().isAvailable();
  }
}
