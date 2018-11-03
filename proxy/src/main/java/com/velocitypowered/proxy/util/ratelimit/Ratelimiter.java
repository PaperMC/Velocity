package com.velocitypowered.proxy.util.ratelimit;

import java.net.InetAddress;

/**
 * Allows rate limiting of clients.
 */
public interface Ratelimiter {

  /**
   * Determines whether or not to allow the connection.
   * @param address the address to rate limit
   * @return true if allowed, false if not
   */
  boolean attempt(InetAddress address);
}
