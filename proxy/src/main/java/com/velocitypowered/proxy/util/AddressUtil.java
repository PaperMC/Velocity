package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.net.InetAddresses;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.unix.DomainSocketAddress;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;

public final class AddressUtil {
  private AddressUtil() {
    throw new AssertionError();
  }

  /**
   * Attempts to parse a socket address of the form {@code 127.0.0.1:25565}. The returned
   * {@link SocketAddress} is not resolved if it is a {@link InetSocketAddress}.
   *
   * @param ip the IP to parse
   * @return the parsed address
   */
  public static SocketAddress parseAddress(String ip) {
    if (ip.startsWith("unix://") && Epoll.isAvailable()) {
      return new DomainSocketAddress(ip.substring("unix://".length()));
    }
    URI uri = URI.create("tcp://" + ip);
    try {
      InetAddress ia = InetAddresses.forUriString(uri.getHost());
      return new InetSocketAddress(ia, uri.getPort());
    } catch (IllegalArgumentException e) {
      return InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort());
    }
  }

  /**
   * Attempts to parse a socket address of the form {@code 127.0.0.1:25565}. The returned
   * {@link SocketAddress} is resolved if it is a {@link InetSocketAddress}.
   *
   * @param ip the IP to parse
   * @return the parsed address
   */
  public static SocketAddress parseAndResolveAddress(String ip) {
    if (ip.startsWith("unix://") && Epoll.isAvailable()) {
      return new DomainSocketAddress(ip.substring("unix://".length()));
    }
    Preconditions.checkNotNull(ip, "ip");
    URI uri = URI.create("tcp://" + ip);
    return new InetSocketAddress(uri.getHost(), uri.getPort());
  }
}
