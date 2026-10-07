/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

package com.velocitypowered.proxy.util.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * A rate-limiter based on a Caffeine {@link Cache}.
 *
 * <p>Each key may make up to {@code permits} attempts within the given time window, with permits
 * being replenished at a steady rate (a generic cell rate algorithm). With a single permit, this
 * allows one attempt per time window.</p>
 */
public class CaffeineCacheRatelimiter<T> implements Ratelimiter<T> {

  // Maps each key to its theoretical arrival time, in ticker nanos.
  private final Cache<T, Long> expiringCache;
  private final Ticker ticker;
  private final long emissionIntervalNanos;
  private final long burstToleranceNanos;

  CaffeineCacheRatelimiter(long time, TimeUnit unit) {
    this(time, unit, 1);
  }

  CaffeineCacheRatelimiter(long time, TimeUnit unit, int permits) {
    this(time, unit, permits, Ticker.systemTicker());
  }

  @VisibleForTesting
  CaffeineCacheRatelimiter(long time, TimeUnit unit, Ticker ticker) {
    this(time, unit, 1, ticker);
  }

  @VisibleForTesting
  CaffeineCacheRatelimiter(long time, TimeUnit unit, int permits, Ticker ticker) {
    Preconditions.checkNotNull(unit, "unit");
    Preconditions.checkNotNull(ticker, "ticker");
    Preconditions.checkArgument(permits > 0, "permits must be positive");
    long windowNanos = unit.toNanos(time);
    this.ticker = ticker;
    this.emissionIntervalNanos = windowNanos / permits;
    this.burstToleranceNanos = windowNanos - emissionIntervalNanos;
    this.expiringCache = Caffeine.newBuilder()
        .ticker(ticker)
        .expireAfter(new Expiry<T, Long>() {
          @Override
          public long expireAfterCreate(T key, Long tat, long currentTime) {
            return Math.max(0, tat - currentTime);
          }

          @Override
          public long expireAfterUpdate(T key, Long tat, long currentTime,
              long currentDuration) {
            return Math.max(0, tat - currentTime);
          }

          @Override
          public long expireAfterRead(T key, Long tat, long currentTime,
              long currentDuration) {
            return currentDuration;
          }
        })
        .build();
  }

  /**
   * Attempts to rate-limit the object.
   *
   * @param key the object to rate limit
   * @return true if we should allow the object, false if we should rate-limit
   */
  @Override
  public boolean attempt(@NotNull T key) {
    long now = ticker.read();
    boolean[] allowed = new boolean[1];
    expiringCache.asMap().compute(key, (k, prev) -> {
      long tat = prev == null ? now : Math.max(prev, now);
      if (tat - now > burstToleranceNanos) {
        return prev;
      }
      allowed[0] = true;
      return tat + emissionIntervalNanos;
    });
    return allowed[0];
  }
}
