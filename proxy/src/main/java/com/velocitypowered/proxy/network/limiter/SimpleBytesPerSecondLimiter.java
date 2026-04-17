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

package com.velocitypowered.proxy.network.limiter;

import com.velocitypowered.proxy.util.IntervalledCounter;
import org.jspecify.annotations.Nullable;

/**
 * A moving-window limiter over a configurable number of seconds.
 * It enforces both packets-per-second and average bytes-per-second limits.
 * The effective cap over the full window equals limitPerSecond * windowSeconds.
 */
public final class SimpleBytesPerSecondLimiter implements PacketLimiter {
  @Nullable
  private final IntervalledCounter bytesCounter;
  @Nullable
  private final IntervalledCounter packetsCounter;
  private final int packetsPerSecond;
  private final int bytesPerSecond;

  /**
   * Creates a new SimpleBytesPerSecondLimiter.
   *
   * @param packetsPerSecond maximum average packets per second allowed (> 0)
   * @param bytesPerSecond maximum average bytes per second allowed (> 0)
   * @param windowSeconds number of seconds in the moving window (> 0)
   */
  public SimpleBytesPerSecondLimiter(int packetsPerSecond, int bytesPerSecond, int windowSeconds) {
    this.packetsPerSecond = packetsPerSecond;
    if (windowSeconds <= 0) {
      throw new IllegalArgumentException("windowSeconds must be > 0");
    }
    this.bytesPerSecond = bytesPerSecond;
    this.packetsCounter = packetsPerSecond > 0 ? new IntervalledCounter((long) (windowSeconds * 1.0e9)) : null;
    this.bytesCounter = bytesPerSecond > 0 ? new IntervalledCounter((long) (windowSeconds * 1.0e9)) : null;

  }

  /**
   * Records the given payload length as one packet and returns whether it is allowed.
   */
  @SuppressWarnings("RedundantIfStatement")
  @Override
  public boolean account(int bytes) {
    long currTime = System.nanoTime();
    if (packetsCounter != null) {
      packetsCounter.updateAndAdd(1, currTime);
      if (packetsCounter.getRate() > packetsPerSecond) {
        return false;
      }
    }

    if (bytesCounter != null) {
      bytesCounter.updateAndAdd(bytes, currTime);
      if (bytesCounter.getRate() > bytesPerSecond) {
        return false;
      }
    }

    return true;
  }
}
