/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.util.AddressUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class HostPatternMatchingTest {
  @Test
  void testOneWildcardMatches() {
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.example.com", "play.example.com"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.example.com", "a.example.com"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("b.*.example.com", "b.a.example.com"));
  }

  @Test
  void testMultipleWildcardsMatch() {
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.*.example.com", "a.b.example.com"));
  }

  @Test
  void testDifferentNumberOfLabelsDoNotMatch() {
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("*.example.com", "a.b.example.com"));
  }

  @Test
  void testWildcardsDoNotMatchApexDomain() {
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("*.example.com", "example.com"));
  }

  @Test
  void testExactMatchesMatch() {
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("example.com", "example.com"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("play.example.com", "play.example.com"));
  }

  @Test
  void testDifferentDomainsDoNotMatch() {
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("otherdomain.com", "example.com"));
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("a.otherdomain.com", "a.example.com"));
  }

  @Test
  void testMalformedArgumentsDoNotMatch() {
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern(null, "example.com"));
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern("example.com", null));
    Assertions.assertFalse(AddressUtil.isHostMatchingPattern(null, null));
  }

  @Test
  void testCaseInsensitivityMatches() {
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("Example.COM", "example.com"));
    Assertions.assertTrue(AddressUtil.isHostMatchingPattern("*.Example.com", "play.EXAMPLE.com"));
  }
}
