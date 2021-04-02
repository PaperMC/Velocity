/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Ticker;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.net.InetAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * A simple rate-limiter based on a Guava {@link Cache}.
 */
public class GuavaCacheRatelimiter implements Ratelimiter {

  private final Cache<InetAddress, Long> expiringCache;
  private final long timeoutNanos;

  GuavaCacheRatelimiter(long time, TimeUnit unit) {
    this(time, unit, Ticker.systemTicker());
  }

  @VisibleForTesting
  GuavaCacheRatelimiter(long time, TimeUnit unit, Ticker ticker) {
    Preconditions.checkNotNull(unit, "unit");
    Preconditions.checkNotNull(ticker, "ticker");
    this.timeoutNanos = unit.toNanos(time);
    this.expiringCache = CacheBuilder.newBuilder()
        .ticker(ticker)
        .concurrencyLevel(Runtime.getRuntime().availableProcessors())
        .expireAfterWrite(time, unit)
        .build();
  }

  /**
   * Attempts to rate-limit the client.
   *
   * @param address the address to rate limit
   * @return true if we should allow the client, false if we should rate-limit
   */
  @Override
  public boolean attempt(InetAddress address) {
    Preconditions.checkNotNull(address, "address");
    long expectedNewValue = System.nanoTime() + timeoutNanos;
    long last;
    try {
      last = expiringCache.get(address, () -> expectedNewValue);
    } catch (ExecutionException e) {
      // It should be impossible for this to fail.
      throw new AssertionError(e);
    }
    return expectedNewValue == last;
  }
}
