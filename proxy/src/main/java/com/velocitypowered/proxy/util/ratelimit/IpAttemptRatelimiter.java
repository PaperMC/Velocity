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

import com.github.benmanes.caffeine.cache.Ticker;
import com.google.common.annotations.VisibleForTesting;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Rate-limits connection attempts by IP address.
 *
 * <p>IPv4 addresses are limited individually. As a single IPv6 subscriber usually controls at
 * least an entire /64, IPv6 addresses are limited per /64 and additionally per /48, so that
 * subscribers that have been assigned a /48 cannot trivially bypass the rate limit. The number of
 * attempts allowed per time window for each prefix is a multiple of what a single IPv4 address
 * is allowed. A multiplier of zero or less disables limiting on that prefix.
 */
final class IpAttemptRatelimiter implements Ratelimiter<InetAddress> {

  private final Ratelimiter<InetAddress> ipv4Limiter;
  private final @Nullable Ratelimiter<Long> ipv6Slash64Limiter;
  private final @Nullable Ratelimiter<Long> ipv6Slash48Limiter;

  IpAttemptRatelimiter(long ms, int slash64Multiplier, int slash48Multiplier) {
    this(ms, slash64Multiplier, slash48Multiplier, Ticker.systemTicker());
  }

  @VisibleForTesting
  IpAttemptRatelimiter(long ms, int slash64Multiplier, int slash48Multiplier, Ticker ticker) {
    this.ipv4Limiter = new CaffeineCacheRatelimiter<>(ms, TimeUnit.MILLISECONDS, 1, ticker);
    this.ipv6Slash64Limiter = slash64Multiplier <= 0 ? null
        : new CaffeineCacheRatelimiter<>(ms, TimeUnit.MILLISECONDS, slash64Multiplier, ticker);
    this.ipv6Slash48Limiter = slash48Multiplier <= 0 ? null
        : new CaffeineCacheRatelimiter<>(ms, TimeUnit.MILLISECONDS, slash48Multiplier, ticker);
  }

  @Override
  public boolean attempt(@NotNull InetAddress key) {
    if (!(key instanceof Inet6Address)) {
      return ipv4Limiter.attempt(key);
    }

    long networkPrefix = ByteBuffer.wrap(key.getAddress()).getLong();
    // Check the narrowest prefix first, so that a single /64 that is already being limited does
    // not use up the attempts of the rest of its /48.
    if (ipv6Slash64Limiter != null && !ipv6Slash64Limiter.attempt(networkPrefix)) {
      return false;
    }
    return ipv6Slash48Limiter == null
        || ipv6Slash48Limiter.attempt(networkPrefix & 0xFFFFFFFFFFFF0000L);
  }
}
