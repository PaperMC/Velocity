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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;

public final class AddressUtil {
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
    try {
      InetAddress ia = InetAddresses.forUriString(uri.getHost());
      return new InetSocketAddress(ia, uri.getPort());
    } catch (IllegalArgumentException e) {
      return InetSocketAddress.createUnresolved(uri.getHost(), uri.getPort());
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
    return new InetSocketAddress(uri.getHost(), uri.getPort());
  }
}
