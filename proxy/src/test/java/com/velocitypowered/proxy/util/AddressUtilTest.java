/*
 * Copyright (C) 2026 Velocity Contributors
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class AddressUtilTest {

  @Test
  public void testIpAndPort() {
    var result = AddressUtil.parseAndResolveAddress("127.0.0.1:25565");
    Assertions.assertEquals("127.0.0.1", result.getAddress().getHostAddress());
    Assertions.assertEquals(25565, result.getPort());
    Assertions.assertFalse(result.isUnresolved());
  }

  @Test
  public void testV6IpAndPort() {
    var result = AddressUtil.parseAndResolveAddress("[::1]:25565");
    Assertions.assertEquals("0:0:0:0:0:0:0:1", result.getAddress().getHostAddress());
    Assertions.assertEquals(25565, result.getPort());
    Assertions.assertFalse(result.isUnresolved());
  }

  @Test
  public void testResolveHostname() {
    var result = AddressUtil.parseAndResolveAddress("localhost:25565");
    Assertions.assertFalse(result.isUnresolved());
  }
}
