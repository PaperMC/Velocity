/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerHealthInfo;
import com.velocitypowered.api.proxy.server.ServerHealthStatus;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Fired when a server's health status changes.
 *
 * @since 3.5.0
 */
public final class ServerHealthChangeEvent {

  private final RegisteredServer server;
  private final ServerHealthStatus previousStatus;
  private final ServerHealthStatus newStatus;
  private final ServerHealthInfo healthInfo;

  /**
   * Creates a new server health change event.
   *
   * @param server the server whose health changed
   * @param previousStatus the previous health status
   * @param newStatus the new health status
   * @param healthInfo the current health information
   */
  public ServerHealthChangeEvent(
      @NonNull RegisteredServer server,
      @NonNull ServerHealthStatus previousStatus,
      @NonNull ServerHealthStatus newStatus,
      @NonNull ServerHealthInfo healthInfo) {
    this.server = server;
    this.previousStatus = previousStatus;
    this.newStatus = newStatus;
    this.healthInfo = healthInfo;
  }

  /**
   * Returns the server whose health status changed.
   *
   * @return the server
   */
  public @NonNull RegisteredServer getServer() {
    return server;
  }

  /**
   * Returns the previous health status.
   *
   * @return the previous status
   */
  public @NonNull ServerHealthStatus getPreviousStatus() {
    return previousStatus;
  }

  /**
   * Returns the new health status.
   *
   * @return the new status
   */
  public @NonNull ServerHealthStatus getNewStatus() {
    return newStatus;
  }

  /**
   * Returns the current health information.
   *
   * @return the health info
   */
  public @NonNull ServerHealthInfo getHealthInfo() {
    return healthInfo;
  }

  /**
   * Returns whether the server became unhealthy.
   *
   * @return {@code true} if the server became unhealthy
   */
  public boolean becameUnhealthy() {
    return previousStatus != ServerHealthStatus.UNHEALTHY
        && newStatus == ServerHealthStatus.UNHEALTHY;
  }

  /**
   * Returns whether the server recovered from an unhealthy state.
   *
   * @return {@code true} if the server recovered
   */
  public boolean recovered() {
    return previousStatus == ServerHealthStatus.UNHEALTHY
        && newStatus == ServerHealthStatus.HEALTHY;
  }

  @Override
  public String toString() {
    return "ServerHealthChangeEvent{"
        + "server=" + server.getServerInfo().getName()
        + ", previousStatus=" + previousStatus
        + ", newStatus=" + newStatus
        + '}';
  }
}

