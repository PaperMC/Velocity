/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import java.time.Instant;
import java.util.Optional;

/**
 * Contains detailed health information about a registered server.
 *
 * @since 3.5.0
 */
public interface ServerHealthInfo {

  /**
   * Returns the current health status of the server.
   *
   * @return the health status
   */
  ServerHealthStatus getStatus();

  /**
   * Returns the last recorded ping latency in milliseconds.
   *
   * @return the latency in milliseconds, or -1 if unknown
   */
  long getLatencyMs();

  /**
   * Returns the number of consecutive failed health checks.
   *
   * @return the number of consecutive failures
   */
  int getConsecutiveFailures();

  /**
   * Returns the timestamp of the last successful health check.
   *
   * @return the timestamp, or empty if never successful
   */
  Optional<Instant> getLastSuccessfulCheck();

  /**
   * Returns the timestamp of the last health check attempt.
   *
   * @return the timestamp, or empty if never checked
   */
  Optional<Instant> getLastCheckAttempt();

  /**
   * Returns the reason for the last failure, if any.
   *
   * @return the failure reason, or empty if no failure
   */
  Optional<String> getLastFailureReason();

  /**
   * Returns whether the server is considered healthy.
   * This is a convenience method equivalent to {@code getStatus() == ServerHealthStatus.HEALTHY}.
   *
   * @return {@code true} if the server is healthy
   */
  default boolean isHealthy() {
    return getStatus() == ServerHealthStatus.HEALTHY;
  }

  /**
   * Returns whether the server is available for player connections.
   * A server is available if its status is either HEALTHY or DEGRADED.
   *
   * @return {@code true} if the server is available
   */
  default boolean isAvailable() {
    ServerHealthStatus status = getStatus();
    return status == ServerHealthStatus.HEALTHY || status == ServerHealthStatus.DEGRADED;
  }
}

