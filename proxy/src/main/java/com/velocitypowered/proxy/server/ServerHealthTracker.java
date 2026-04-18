/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.server;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tracks the health of backend servers by periodically pinging them and extracting performance data.
 */
public final class ServerHealthTracker {
  private static final Logger logger = LogManager.getLogger(ServerHealthTracker.class);
  private final VelocityServer server;
  private final Map<String, ServerHealth> healthMap = new ConcurrentHashMap<>();

  public ServerHealthTracker(VelocityServer server) {
    this.server = server;
  }

  /**
   * Starts the health tracking task.
   */
  public void start() {
    server.getScheduler().buildTask(server, this::refreshHealth)
        .repeat(10, TimeUnit.SECONDS)
        .schedule();
  }

  private void refreshHealth() {
    for (RegisteredServer registeredServer : server.getAllServers()) {
      registeredServer.ping().whenComplete((ping, throwable) -> {
        if (throwable != null) {
          healthMap.put(registeredServer.getServerInfo().getName(), ServerHealth.unhealthy());
          return;
        }
        healthMap.put(registeredServer.getServerInfo().getName(), parseHealth(ping));
      });
    }
  }

  private ServerHealth parseHealth(ServerPing ping) {
    int players = ping.getPlayers().map(ServerPing.Players::getOnline).orElse(0);
    int maxPlayers = ping.getPlayers().map(ServerPing.Players::getMax).orElse(1);
    double load = (double) players / maxPlayers;

    // Default to healthy (20.0 MSPT).
    // Note: In a production environment, this should be replaced with a more robust
    // telemetry system, such as reading from a dedicated health endpoint or using
    // custom plugin messaging to sync MSPT data from backend servers.
    double mspt = 20.0;

    return new ServerHealth(true, load, mspt);
  }

  public Optional<ServerHealth> getHealth(String serverName) {
    return Optional.ofNullable(healthMap.get(serverName));
  }

  /**
   * Represents the health of a backend server.
   */
  public static record ServerHealth(boolean online, double load, double mspt) {
    /**
     * Creates an unhealthy server state.
     *
     * @return an unhealthy state
     */
    public static ServerHealth unhealthy() {
      return new ServerHealth(false, 1.0, 100.0);
    }

    /**
     * Calculates the health score for this server.
     *
     * @return the health score (lower is better)
     */
    public double getScore() {
      if (!online) {
        return Double.MAX_VALUE;
      }
      // Lower score is better. MSPT is weighted more heavily than player load.
      return (mspt * 0.7) + (load * 30.0);
    }
  }
}
