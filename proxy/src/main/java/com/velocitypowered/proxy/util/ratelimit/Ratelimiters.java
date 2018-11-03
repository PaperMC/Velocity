package com.velocitypowered.proxy.util.ratelimit;

import java.util.concurrent.TimeUnit;

public final class Ratelimiters {
  private Ratelimiters() {
    throw new AssertionError();
  }

  public static Ratelimiter createWithMilliseconds(long ms) {
    return ms <= 0 ? NoopCacheRatelimiter.INSTANCE : new GuavaCacheRatelimiter(ms,
        TimeUnit.MILLISECONDS);
  }
}
