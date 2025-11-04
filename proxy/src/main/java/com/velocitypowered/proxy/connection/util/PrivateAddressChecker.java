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

package com.velocitypowered.proxy.connection.util;

import java.net.InetAddress;

/**
 * Utility class for checking whether an IP address belongs to a private network as defined by
 * RFC 1918. Such addresses are non-routable and only used within local networks, such as VPNs
 * or internal systems. This can be used to bypass certain validation steps (e.g. public
 * IP matching for Mojang authentication) for trusted clients.
 */
public final class PrivateAddressChecker {

  private PrivateAddressChecker() {
    // Utility class
  }

  /**
   * Checks if the provided IP address falls within one of the RFC 1918 private IPv4 ranges:
   *
   * <ul>
   *   <li>10.0.0.0     to 10.255.255.255     (10.0.0.0/8)</li>
   *   <li>172.16.0.0   to 172.31.255.255     (172.16.0.0/12)</li>
   *   <li>192.168.0.0  to 192.168.255.255    (192.168.0.0/16)</li>
   * </ul>
   *
   * @param address the IP address to evaluate
   * @return {@code true} if the address is private, {@code false} otherwise
   */
   public static boolean isPrivateIP(InetAddress address) {
    byte[] ip = address.getAddress();

    // 10.0.0.0/8 range: First octet = 10
    if (ip[0] == 10) {
      return true;
    }

    // 172.16.0.0/12 range: First octet = 172, second octet = 16–31
    if (ip[0] == (byte) 172 && (ip[1] & 0xF0) == 16) {
      return true;
    }

    // 192.168.0.0/16 range: First octet = 192, second octet = 168
    if (ip[0] == (byte) 192 && ip[1] == (byte) 168) {
      return true;
    }

    // Address is not in any private range
    return false;
  }
}
