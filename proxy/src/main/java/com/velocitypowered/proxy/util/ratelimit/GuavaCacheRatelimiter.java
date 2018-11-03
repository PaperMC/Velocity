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
