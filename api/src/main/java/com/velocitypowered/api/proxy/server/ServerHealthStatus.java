/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

/**
 * Represents the health status of a registered server.
 *
 * @since 3.5.0
 */
public enum ServerHealthStatus {
  /**
   * The server is healthy and responding to pings normally.
   */
  HEALTHY,

  /**
   * The server is responding but with degraded performance (high latency).
   */
  DEGRADED,

  /**
   * The server is not responding to pings or has exceeded the unhealthy threshold.
   */
  UNHEALTHY,

  /**
   * The server health status is unknown (health checking is disabled or not yet checked).
   */
  UNKNOWN
}

