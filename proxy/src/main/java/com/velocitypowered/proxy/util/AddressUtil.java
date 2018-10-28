package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.URI;

public class AddressUtil {
  private AddressUtil() {
    throw new AssertionError();
  }

  /**
   * Attempts to parse an IP address of the form <code>127.0.0.1:25565</code>.
   *
   * @param ip the IP to parse
   * @return the parsed address
   */
  public static InetSocketAddress parseAddress(String ip) {
    Preconditions.checkNotNull(ip, "ip");
    URI uri = URI.create("tcp://" + ip);
    return new InetSocketAddress(uri.getHost(), uri.getPort());
  }
}
