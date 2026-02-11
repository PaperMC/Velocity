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

import com.velocitypowered.api.proxy.server.ServerHealthInfo;
import com.velocitypowered.api.proxy.server.ServerHealthStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implementation of {@link ServerHealthInfo} with thread-safe mutable state.
 */
public class VelocityServerHealthInfo implements ServerHealthInfo {

  private final AtomicReference<ServerHealthStatus> status =
      new AtomicReference<>(ServerHealthStatus.UNKNOWN);
  private final AtomicLong latencyMs = new AtomicLong(-1);
  private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
  private final AtomicReference<@Nullable Instant> lastSuccessfulCheck = new AtomicReference<>(null);
  private final AtomicReference<@Nullable Instant> lastCheckAttempt = new AtomicReference<>(null);
  private final AtomicReference<@Nullable String> lastFailureReason = new AtomicReference<>(null);

  @Override
  public ServerHealthStatus getStatus() {
    return status.get();
  }

  @Override
  public long getLatencyMs() {
    return latencyMs.get();
  }

  @Override
  public int getConsecutiveFailures() {
    return consecutiveFailures.get();
  }

  @Override
  public Optional<Instant> getLastSuccessfulCheck() {
    return Optional.ofNullable(lastSuccessfulCheck.get());
  }

  @Override
  public Optional<Instant> getLastCheckAttempt() {
    return Optional.ofNullable(lastCheckAttempt.get());
  }

  @Override
  public Optional<String> getLastFailureReason() {
    return Optional.ofNullable(lastFailureReason.get());
  }

  /**
   * Records a successful health check.
   *
   * @param latency the ping latency in milliseconds
   * @param degradedThresholdMs the latency threshold for degraded status
   */
  public void recordSuccess(long latency, long degradedThresholdMs) {
    Instant now = Instant.now();
    latencyMs.set(latency);
    consecutiveFailures.set(0);
    lastSuccessfulCheck.set(now);
    lastCheckAttempt.set(now);
    lastFailureReason.set(null);

    if (latency > degradedThresholdMs) {
      status.set(ServerHealthStatus.DEGRADED);
    } else {
      status.set(ServerHealthStatus.HEALTHY);
    }
  }

  /**
   * Records a failed health check.
   *
   * @param reason the failure reason
   * @param unhealthyThreshold the number of failures before marking unhealthy
   */
  public void recordFailure(String reason, int unhealthyThreshold) {
    Instant now = Instant.now();
    lastCheckAttempt.set(now);
    lastFailureReason.set(reason);
    int failures = consecutiveFailures.incrementAndGet();

    if (failures >= unhealthyThreshold) {
      status.set(ServerHealthStatus.UNHEALTHY);
    }
  }

  /**
   * Resets the health info to unknown state.
   */
  public void reset() {
    status.set(ServerHealthStatus.UNKNOWN);
    latencyMs.set(-1);
    consecutiveFailures.set(0);
    lastSuccessfulCheck.set(null);
    lastCheckAttempt.set(null);
    lastFailureReason.set(null);
  }

  @Override
  public String toString() {
    return "VelocityServerHealthInfo{"
        + "status=" + status.get()
        + ", latencyMs=" + latencyMs.get()
        + ", consecutiveFailures=" + consecutiveFailures.get()
        + ", lastSuccessfulCheck=" + lastSuccessfulCheck.get()
        + ", lastFailureReason=" + lastFailureReason.get()
        + '}';
  }
}

