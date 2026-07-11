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
import com.google.common.net.InetAddresses;
import com.velocitypowered.proxy.VelocityServer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

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

  /**
   * Tests whether a host matches a pattern whose labels may be the
   * wildcard {@code "*"}. Each {@code "*"} matches exactly one label; all other
   * labels match case-insensitively.
   *
   * @param pattern the pattern to match against, for example, {@code *.example.com}
   * @param host the virtual host to test, for example, {@code play.example.com}
   * @return true if the host matches the pattern, false otherwise
   */
  public static boolean isHostMatchingPattern(@Nullable String pattern, @Nullable String host) {
    if (host == null || pattern == null) {
      return false;
    }

    String[] patternDomains = pattern.split("\\.");
    String[] strDomains = host.split("\\.");

    if (patternDomains.length != strDomains.length) {
      return false;
    }

    for (int i = 0; i < patternDomains.length; i++) {
      String patternDomain = patternDomains[i];
      String strDomain = strDomains[i];

      if (!patternDomain.equals("*") && !strDomain.equalsIgnoreCase(patternDomain)) {
        return false;
      }
    }

    return true;
  }

  /**
   * Resolves the list of servers configured for a given virtual host via forced hosts. An exact
   * match on the virtual host is preferred. Then, if none exist, the configured forced host patterns are
   * checked in turn and the first matching pattern's servers are returned.
   *
   * @param server the proxy server providing the forced host configuration
   * @param virtualHostStr the virtual host the client connected with
   * @return the servers for the matching forced host, or {@link Optional#empty()} if none match
   */
  public static Optional<List<String>> resolveForcedHostServers(VelocityServer server, String virtualHostStr) {
    Map<String, List<String>> forcedHosts = server.getConfiguration().getForcedHosts();

    // Check for exact match
    List<String> exactMatch = forcedHosts.get(virtualHostStr);

    if (exactMatch != null) {
      return Optional.of(exactMatch);
    }

    // Check for pattern match
    for (Map.Entry<String, List<String>> entry : forcedHosts.entrySet()) {
      String virtualHostPattern = entry.getKey();

      if (AddressUtil.isHostMatchingPattern(virtualHostPattern, virtualHostStr)) {
        return Optional.of(entry.getValue());
      }
    }

    // No match
    return Optional.empty();
  }
}
