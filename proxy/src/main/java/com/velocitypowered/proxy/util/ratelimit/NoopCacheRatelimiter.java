package com.velocitypowered.proxy.util.ratelimit;

import java.net.InetAddress;

/**
 * A {@link Ratelimiter} that does no rate-limiting.
 */
enum NoopCacheRatelimiter implements Ratelimiter {
  INSTANCE;

  @Override
  public boolean attempt(InetAddress address) {
    return true;
  }
}
