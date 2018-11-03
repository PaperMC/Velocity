package com.velocitypowered.proxy.util.ratelimit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.base.Ticker;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class GuavaCacheRatelimiterTest {

  @Test
  void attemptZero() {
    Ratelimiter noRatelimiter = new GuavaCacheRatelimiter(0, TimeUnit.MILLISECONDS);
    assertTrue(noRatelimiter.attempt(InetAddress.getLoopbackAddress()));
    assertTrue(noRatelimiter.attempt(InetAddress.getLoopbackAddress()));
  }

  @Test
  void attemptOne() {
    long base = System.nanoTime();
    AtomicLong extra = new AtomicLong();
    Ticker testTicker = new Ticker() {
      @Override
      public long read() {
        return base + extra.get();
      }
    };
    Ratelimiter ratelimiter = new GuavaCacheRatelimiter(1000, TimeUnit.MILLISECONDS, testTicker);
    assertTrue(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
    assertFalse(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
    extra.addAndGet(TimeUnit.SECONDS.toNanos(2));
    assertTrue(ratelimiter.attempt(InetAddress.getLoopbackAddress()));
  }

}
