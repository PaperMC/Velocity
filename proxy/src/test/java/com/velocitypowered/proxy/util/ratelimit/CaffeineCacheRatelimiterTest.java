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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.github.benmanes.caffeine.cache.Ticker;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class CaffeineCacheRatelimiterTest {

  @Test
  void attemptZero() {
    Ratelimiter noRatelimiter = new CaffeineCacheRatelimiter(0, TimeUnit.MILLISECONDS);
    assertTrue(noRatelimiter.attempt(InetAddress.getLoopbackAddress()));
    assertTrue(noRatelimiter.attempt(InetAddress.getLoopbackAddress()));
  }

  @Test
  void attemptOne() {
    long base = System.nanoTime();
    AtomicLong extra = new AtomicLong();
    Ticker testTicker = () -> base + extra.get();
    Ratelimiter ratelimiter = new CaffeineCacheRatelimiter(1000, TimeUnit.MILLISECONDS, testTicker);
    assertTrue(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
    assertFalse(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
    extra.addAndGet(TimeUnit.SECONDS.toNanos(2));
    assertTrue(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
  }

}
