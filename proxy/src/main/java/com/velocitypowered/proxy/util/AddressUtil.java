/*
 * Copyright (C) 2018 Velocity Contributors
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
  private static final int DEFAULT_MINECRAFT_PORT = 25565;

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
    if (uri.getHost() == null) {
      throw new IllegalStateException("Invalid hostname/IP " + ip);
    }

    int port = uri.getPort() == -1 ? DEFAULT_MINECRAFT_PORT : uri.getPort();
    return new InetSocketAddress(uri.getHost(), port);
  }
}
