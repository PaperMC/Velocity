/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.server.health;

import com.velocitypowered.api.event.proxy.server.ServerHealthChangeEvent;
import com.velocitypowered.api.proxy.server.PingOptions;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerHealthStatus;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages periodic health checks for all registered servers.
 */
public class ServerHealthChecker {

  private static final Logger logger = LogManager.getLogger(ServerHealthChecker.class);

  private final VelocityServer server;
  private ScheduledTask healthCheckTask;

  /**
   * Creates a new server health checker.
   *
   * @param server the velocity server instance
   */
  public ServerHealthChecker(VelocityServer server) {
    this.server = server;
  }

  /**
   * Starts the health check scheduler.
   */
  public void start() {
    VelocityConfiguration.HealthCheck config = server.getConfiguration().getHealthCheck();
    if (!config.isEnabled()) {
      logger.info("Server health checking is disabled");
      return;
    }

    long intervalSeconds = config.getIntervalSeconds();
    logger.info("Starting server health checker with {}s interval", intervalSeconds);

    this.healthCheckTask = server.getScheduler()
        .buildTask(server, this::checkAllServers)
        .delay(intervalSeconds, TimeUnit.SECONDS)
        .repeat(intervalSeconds, TimeUnit.SECONDS)
        .schedule();
  }

  /**
   * Stops the health check scheduler.
   */
  public void stop() {
    if (healthCheckTask != null) {
      healthCheckTask.cancel();
      healthCheckTask = null;
      logger.info("Server health checker stopped");
    }
  }

  /**
   * Checks the health of all registered servers.
   */
  private void checkAllServers() {
    VelocityConfiguration.HealthCheck config = server.getConfiguration().getHealthCheck();

    for (RegisteredServer registeredServer : server.getAllServers()) {
      if (registeredServer instanceof VelocityRegisteredServer vrs) {
        checkServer(vrs, config);
      }
    }
  }

  /**
   * Checks the health of a single server.
   *
   * @param registeredServer the server to check
   * @param config the health check configuration
   */
  private void checkServer(VelocityRegisteredServer registeredServer,
                           VelocityConfiguration.HealthCheck config) {
    String serverName = registeredServer.getServerInfo().getName();
    VelocityServerHealthInfo healthInfo = registeredServer.getHealthInfo();
    ServerHealthStatus previousStatus = healthInfo.getStatus();

    PingOptions pingOptions = PingOptions.builder()
        .timeout(config.getTimeoutMs(), TimeUnit.MILLISECONDS)
        .build();

    long startTime = System.currentTimeMillis();

    registeredServer.ping(pingOptions).whenComplete((result, throwable) -> {
      long latency = System.currentTimeMillis() - startTime;

      if (throwable != null) {
        String reason = throwable.getMessage() != null
            ? throwable.getMessage()
            : throwable.getClass().getSimpleName();
        healthInfo.recordFailure(reason, config.getUnhealthyThreshold());

        if (logger.isDebugEnabled()) {
          logger.debug("Health check failed for {}: {}", serverName, reason);
        }
      } else {
        healthInfo.recordSuccess(latency, config.getDegradedThresholdMs());

        if (logger.isDebugEnabled()) {
          logger.debug("Health check succeeded for {} ({}ms)", serverName, latency);
        }
      }

      ServerHealthStatus newStatus = healthInfo.getStatus();
      if (previousStatus != newStatus) {
        logger.info("Server {} health status changed: {} -> {}",
            serverName, previousStatus, newStatus);

        server.getEventManager().fireAndForget(
            new ServerHealthChangeEvent(registeredServer, previousStatus, newStatus, healthInfo));
      }
    });
  }

  /**
   * Manually triggers a health check for a specific server.
   *
   * @param registeredServer the server to check
   */
  public void checkServerNow(VelocityRegisteredServer registeredServer) {
    VelocityConfiguration.HealthCheck config = server.getConfiguration().getHealthCheck();
    checkServer(registeredServer, config);
  }
}

