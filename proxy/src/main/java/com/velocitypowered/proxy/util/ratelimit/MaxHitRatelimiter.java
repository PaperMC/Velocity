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
import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simple rate-limiter based on a Caffeine {@link Cache}.
 * This particular version limits the number of actions
 * that can be accomplished in a limited amount of time
 */
public class MaxHitRatelimiter<T> implements Ratelimiter<T> {

  private final Cache<T, AtomicInteger> expiringCache;
  private final int maxLimit;

  MaxHitRatelimiter(long time, int maxLimit, TimeUnit unit) {
    this(time, maxLimit, unit, Ticker.systemTicker());
  }

  @VisibleForTesting
  MaxHitRatelimiter(long time, int maxLimit, TimeUnit unit, Ticker ticker) {
    Preconditions.checkNotNull(unit, "unit");
    Preconditions.checkNotNull(ticker, "ticker");
    this.maxLimit = maxLimit;
    this.expiringCache = Caffeine.newBuilder()
        .ticker(ticker)
        .expireAfterWrite(time, unit)
        .build();
  }

  /**
   * Attempts to rate-limit the client.
   *
   * @param key the key to rate limit
   * @return true if we should allow the client, false if we should rate-limit
   */
  @Override
  public boolean attempt(T key) {
    Preconditions.checkNotNull(key, "address");
    var last = expiringCache.get(key, (address1) -> new AtomicInteger(0));

    return last.incrementAndGet() <= maxLimit;
  }
}
