/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import java.net.InetAddress;
import java.util.concurrent.TimeUnit;

/**
 * Factory to create rate limiters.
 */
public final class Ratelimiters {

  /**
   * How many login attempts a single IPv6 /64 may make relative to a single IPv4 address.
   */
  private static final int IPV6_SLASH_64_MULTIPLIER =
      Integer.getInteger("velocity.login-ratelimit.ipv6-64-multiplier", 1);
  /**
   * How many login attempts a single IPv6 /48 may make relative to a single IPv4 address.
   */
  private static final int IPV6_SLASH_48_MULTIPLIER =
      Integer.getInteger("velocity.login-ratelimit.ipv6-48-multiplier", 32);

  private Ratelimiters() {
    throw new AssertionError();
  }

  @SuppressWarnings("unchecked")
  public static <T> Ratelimiter<T> createWithMilliseconds(long ms) {
    return ms <= 0 ? (Ratelimiter<T>) NoopCacheRatelimiter.INSTANCE : new CaffeineCacheRatelimiter(ms,
        TimeUnit.MILLISECONDS);
  }

  /**
   * Creates a rate limiter for login attempts by IP address, which groups IPv6 addresses by
   * their /64 and /48 prefixes.
   *
   * @param ms the time window in milliseconds a single IPv4 address may make one attempt in
   * @return the rate limiter
   */
  @SuppressWarnings("unchecked")
  public static Ratelimiter<InetAddress> createIpAttemptLimiter(long ms) {
    return ms <= 0 ? (Ratelimiter<InetAddress>) (Ratelimiter<?>) NoopCacheRatelimiter.INSTANCE
        : new IpAttemptRatelimiter(ms, IPV6_SLASH_64_MULTIPLIER, IPV6_SLASH_48_MULTIPLIER);
  }
}
