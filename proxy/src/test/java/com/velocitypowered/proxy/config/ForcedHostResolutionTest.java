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

package com.velocitypowered.proxy.config;

import static com.velocitypowered.proxy.config.VelocityConfiguration.resolveMatchedForcedHost;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ForcedHostResolutionTest {

  private static final Map<String, List<String>> HOSTS = Map.of(
      "play.example.com", List.of("lobby"),
      "factions.example.com", List.of("factions"),
      "hub.example.net", List.of("hub")
  );

  // --- Exact matching (subdomain matching OFF) ---

  @Test
  void exactMatchReturnsKey() {
    assertEquals("play.example.com",
        resolveMatchedForcedHost(HOSTS, "play.example.com", false));
  }

  @Test
  void noMatchReturnsNull() {
    assertNull(resolveMatchedForcedHost(HOSTS, "unknown.example.com", false));
  }

  @Test
  void subdomainDoesNotMatchWhenDisabled() {
    assertNull(resolveMatchedForcedHost(HOSTS, "_dc-srv.abc.play.example.com", false));
  }

  // --- Subdomain matching (enabled) ---

  @Test
  void subdomainMatchesWhenEnabled() {
    assertEquals("play.example.com",
        resolveMatchedForcedHost(HOSTS, "_dc-srv.abc.play.example.com", true));
  }

  @Test
  void exactMatchTakesPriorityOverSubdomain() {
    assertEquals("play.example.com",
        resolveMatchedForcedHost(HOSTS, "play.example.com", true));
  }

  @Test
  void subdomainMatchRequiresDotBoundary() {
    assertNull(resolveMatchedForcedHost(HOSTS, "notplay.example.com", true));
  }

  @Test
  void deepSubdomainMatches() {
    assertEquals("play.example.com",
        resolveMatchedForcedHost(HOSTS, "a.b.c.play.example.com", true));
  }

  @Test
  void emptyVirtualHostReturnsNull() {
    assertNull(resolveMatchedForcedHost(HOSTS, "", true));
  }

  @Test
  void noMatchSubdomainEnabledReturnsNull() {
    assertNull(resolveMatchedForcedHost(HOSTS, "totally.different.org", true));
  }
}
