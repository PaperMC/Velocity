/*
 * Copyright (C) 2026 Velocity Contributors
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
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class IpAttemptRatelimiterTest {

  private final AtomicLong time = new AtomicLong(System.nanoTime());
  private final Ticker ticker = time::get;

  private static InetAddress ip(String address) throws UnknownHostException {
    return InetAddress.getByName(address);
  }

  @Test
  void ipv4AddressesAreLimitedIndividually() throws UnknownHostException {
    Ratelimiter<InetAddress> limiter = new IpAttemptRatelimiter(1000, 1, 4, ticker);
    assertTrue(limiter.attempt(ip("192.0.2.1")));
    assertFalse(limiter.attempt(ip("192.0.2.1")));
    assertTrue(limiter.attempt(ip("192.0.2.2")));
    time.addAndGet(TimeUnit.SECONDS.toNanos(1));
    assertTrue(limiter.attempt(ip("192.0.2.1")));
  }

  @Test
  void ipv6AddressesInSameSlash64ShareLimit() throws UnknownHostException {
    Ratelimiter<InetAddress> limiter = new IpAttemptRatelimiter(1000, 1, 4, ticker);
    assertTrue(limiter.attempt(ip("2001:db8:0:1::1")));
    assertFalse(limiter.attempt(ip("2001:db8:0:1::2")));
    assertFalse(limiter.attempt(ip("2001:db8:0:1:ffff:ffff:ffff:ffff")));
    time.addAndGet(TimeUnit.SECONDS.toNanos(1));
    assertTrue(limiter.attempt(ip("2001:db8:0:1::2")));
  }

  @Test
  void ipv6Slash48AllowsMultipleSlash64s() throws UnknownHostException {
    Ratelimiter<InetAddress> limiter = new IpAttemptRatelimiter(1000, 1, 4, ticker);
    for (int i = 0; i < 4; i++) {
      assertTrue(limiter.attempt(ip("2001:db8:0:" + i + "::1")));
    }
    assertFalse(limiter.attempt(ip("2001:db8:0:4::1")));
    // A different /48 is unaffected.
    assertTrue(limiter.attempt(ip("2001:db8:1:4::1")));
    // Permits replenish at a steady rate. The rejected /64 above has used up its own permit.
    time.addAndGet(TimeUnit.MILLISECONDS.toNanos(250));
    assertFalse(limiter.attempt(ip("2001:db8:0:4::1")));
    assertTrue(limiter.attempt(ip("2001:db8:0:5::1")));
    assertFalse(limiter.attempt(ip("2001:db8:0:6::1")));
  }

  @Test
  void limitedSlash64DoesNotConsumeSlash48() throws UnknownHostException {
    Ratelimiter<InetAddress> limiter = new IpAttemptRatelimiter(1000, 1, 2, ticker);
    assertTrue(limiter.attempt(ip("2001:db8:0:1::1")));
    for (int i = 0; i < 10; i++) {
      assertFalse(limiter.attempt(ip("2001:db8:0:1::1")));
    }
    assertTrue(limiter.attempt(ip("2001:db8:0:2::1")));
  }

  @Test
  void slash64Multiplier() throws UnknownHostException {
    Ratelimiter<InetAddress> limiter = new IpAttemptRatelimiter(1000, 2, 0, ticker);
    assertTrue(limiter.attempt(ip("2001:db8::1")));
    assertTrue(limiter.attempt(ip("2001:db8::2")));
    assertFalse(limiter.attempt(ip("2001:db8::3")));
  }

  @Test
  void ipv4MappedAddressIsTreatedAsIpv4() throws UnknownHostException {
    Ratelimiter<InetAddress> limiter = new IpAttemptRatelimiter(1000, 1, 1, ticker);
    assertTrue(limiter.attempt(ip("::ffff:192.0.2.1")));
    assertFalse(limiter.attempt(ip("192.0.2.1")));
    assertTrue(limiter.attempt(ip("::ffff:192.0.2.2")));
  }
}
