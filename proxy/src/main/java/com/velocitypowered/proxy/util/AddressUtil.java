package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

public final class AddressUtil {
  private static final int DEFAULT_MINECRAFT_PORT = 25565;

  private AddressUtil() {
    throw new AssertionError();
  }

  /**
   * Attempts to parse an IP address of the form {@code 127.0.0.1:25565}. The returned
   * {@link InetSocketAddress} is not resolved.
   *
   * @param ip the IP to parse
   * @return the parsed address
   */
  public static InetSocketAddress parseAddress(String ip) {
    Preconditions.checkNotNull(ip, "ip");
    URI uri = URI.create("tcp://" + ip);
    if (uri.getHost() == null) {
      throw new IllegalStateException("Invalid hostname/IP " + ip);
    }

    int port = uri.getPort() == -1 ? DEFAULT_MINECRAFT_PORT : uri.getPort();
    try {
      InetAddress ia = InetAddresses.forUriString(uri.getHost());
      return new InetSocketAddress(ia, port);
    } catch (IllegalArgumentException e) {
      return InetSocketAddress.createUnresolved(uri.getHost(), port);
    }
  }

  /**
   * Attempts to parse an IP address of the form {@code 127.0.0.1:25565}. The returned
   * {@link InetSocketAddress} is resolved.
   *
   * @param ip the IP to parse
   * @return the parsed address
   */
  public static InetSocketAddress parseAndResolveAddress(String ip) {
    Preconditions.checkNotNull(ip, "ip");
    URI uri = URI.create("tcp://" + ip);
    if (uri.getHost() == null) {
      throw new IllegalStateException("Invalid hostname/IP " + ip);
    }

    int port = uri.getPort() == -1 ? DEFAULT_MINECRAFT_PORT : uri.getPort();
    return new InetSocketAddress(uri.getHost(), port);
  }
}
