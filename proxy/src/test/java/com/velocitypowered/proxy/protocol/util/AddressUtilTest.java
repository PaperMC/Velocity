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

package com.velocitypowered.proxy.protocol.util;

import static com.velocitypowered.proxy.util.AddressUtil.isHostMatchingPattern;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AddressUtilTest {
  @Test
  void testOneWildcardMatches() {
    assertTrue(isHostMatchingPattern("*.example.com", "play.example.com"));
    assertTrue(isHostMatchingPattern("*.example.com", "a.example.com"));
    assertTrue(isHostMatchingPattern("b.*.example.com", "b.a.example.com"));
  }

  @Test
  void testMultipleWildcardsMatch() {
    assertTrue(isHostMatchingPattern("*.*.example.com", "a.b.example.com"));
  }

  @Test
  void testDifferentNumberOfLabelsDoNotMatch() {
    assertFalse(isHostMatchingPattern("*.example.com", "a.b.example.com"));
  }

  @Test
  void testWildcardsDoNotMatchApexDomain() {
    assertFalse(isHostMatchingPattern("*.example.com", "example.com"));
  }

  @Test
  void testExactMatchesMatch() {
    assertTrue(isHostMatchingPattern("example.com", "example.com"));
    assertTrue(isHostMatchingPattern("play.example.com", "play.example.com"));
  }

  @Test
  void testDifferentDomainsDoNotMatch() {
    assertFalse(isHostMatchingPattern("otherdomain.com", "example.com"));
    assertFalse(isHostMatchingPattern("a.otherdomain.com", "a.example.com"));
  }

  @Test
  void testMalformedArgumentsDoNotMatch() {
    assertFalse(isHostMatchingPattern(null, "example.com"));
    assertFalse(isHostMatchingPattern("example.com", null));
    assertFalse(isHostMatchingPattern(null, null));
  }

  @Test
  void testCaseInsensitivityMatches() {
    assertTrue(isHostMatchingPattern("Example.COM", "example.com"));
    assertTrue(isHostMatchingPattern("*.Example.com", "play.EXAMPLE.com"));
  }
}
