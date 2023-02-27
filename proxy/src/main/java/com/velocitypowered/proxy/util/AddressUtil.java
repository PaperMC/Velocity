/*
 * Copyright (C) 2018-2023 Velocity Contributors
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
import java.net.InetSocketAddress;

/**
 * Utilities to parse addresses.
 */
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
    String[] address = ip.split(":");
    if (address.length > 2 || address[0].isEmpty()) {
      throw new IllegalStateException("Invalid hostname/IP " + ip);
    }
    int port = address.length == 2 ? Integer.parseInt(address[1]) : DEFAULT_MINECRAFT_PORT;
    return new InetSocketAddress(address[0], port);
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
    String[] address = ip.split(":");
    if (address.length > 2 || address[0].isEmpty()) {
      throw new IllegalStateException("Invalid hostname/IP " + ip);
    }
    int port = address.length == 2 ? Integer.parseInt(address[1]) : DEFAULT_MINECRAFT_PORT;
    return new InetSocketAddress(address[0], port);
  }
}