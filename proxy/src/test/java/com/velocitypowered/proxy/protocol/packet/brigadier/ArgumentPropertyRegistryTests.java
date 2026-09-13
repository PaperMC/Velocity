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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ArgumentPropertyRegistry} id resolution.
 */
public class ArgumentPropertyRegistryTests {

  @Test
  void identifierEqualityUsesIdentifierString() {
    ArgumentIdentifier a = ArgumentIdentifier.id("minecraft:template_rotation",
        ArgumentIdentifier.mapSet(ProtocolVersion.MINECRAFT_1_19, 46));
    ArgumentIdentifier b = ArgumentIdentifier.id("minecraft:template_rotation",
        ArgumentIdentifier.mapSet(ProtocolVersion.MINECRAFT_1_19, 47));

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertNotEquals(a, ArgumentIdentifier.id("minecraft:heightmap",
        ArgumentIdentifier.mapSet(ProtocolVersion.MINECRAFT_1_19_4, 47)));
  }

  @Test
  void collisionResolutionIsDeterministicAndPrefersVanilla() {
    // Simulates the registry state behind the issue: a mod argument reusing a vanilla id.
    ProtocolVersion version = ProtocolVersion.MINECRAFT_1_21_11;
    ArgumentIdentifier vanilla = ArgumentIdentifier.id("minecraft:template_rotation",
        ArgumentIdentifier.mapSet(version, 50));
    ArgumentIdentifier mod = ArgumentIdentifier.id("forge:enum",
        ArgumentIdentifier.mapSet(version, 50));

    // Registration order must not change the result.
    assertResolvesTo(vanilla, Arrays.asList(vanilla, mod), version, 50);
    assertResolvesTo(vanilla, Arrays.asList(mod, vanilla), version, 50);
  }

  @Test
  void collisionTieBreakIsLexicographicForEqualNamespaces() {
    ProtocolVersion version = ProtocolVersion.MINECRAFT_1_21_11;
    ArgumentIdentifier earlier = ArgumentIdentifier.id("forge:aaa",
        ArgumentIdentifier.mapSet(version, 40));
    ArgumentIdentifier later = ArgumentIdentifier.id("forge:zzz",
        ArgumentIdentifier.mapSet(version, 40));

    assertResolvesTo(earlier, Arrays.asList(later, earlier), version, 40);
    assertResolvesTo(earlier, Arrays.asList(earlier, later), version, 40);
  }

  @Test
  void readIdentifierRoundTripsAllRegisteredIdentifiers() {
    // Exercise a spread of protocols including the newest 26.x line.
    ProtocolVersion[] versions = {
        ProtocolVersion.MINECRAFT_1_19, ProtocolVersion.MINECRAFT_1_19_3,
        ProtocolVersion.MINECRAFT_1_19_4, ProtocolVersion.MINECRAFT_1_20_5,
        ProtocolVersion.MINECRAFT_1_21_6, ProtocolVersion.MINECRAFT_1_21_11,
        ProtocolVersion.MINECRAFT_26_2
    };
    for (ProtocolVersion version : versions) {
      for (ArgumentIdentifier identifier : ArgumentPropertyRegistry.identifiers()) {
        Integer id = identifier.getIdByProtocolVersion(version);
        if (id == null || id < 0) {
          continue; // not in use for this version (removed types and mod args use negative ids)
        }
        ByteBuf buf = Unpooled.buffer();
        ArgumentPropertyRegistry.writeIdentifier(buf, identifier, version);
        assertEquals(identifier, ArgumentPropertyRegistry.readIdentifier(buf, version));
      }
    }
  }

  @Test
  void resolvesTheIdsFromTheIssueToVanillaIdentifiers() {
    ProtocolVersion version = ProtocolVersion.MINECRAFT_1_21_11;
    Map<ProtocolVersion, Int2ObjectMap<ArgumentIdentifier>> index =
        ArgumentPropertyRegistry.buildIndex(ArgumentPropertyRegistry.identifiers(),
            new ArrayList<>());

    assertEquals("minecraft:template_rotation",
        index.get(version).get(50).getIdentifier());
    assertEquals("minecraft:heightmap", index.get(version).get(51).getIdentifier());
  }

  @Test
  void readIdentifierRoundTripsPre119StringPath() {
    // Pre-1.19 argument types are identified by their string, not a numeric id.
    ProtocolVersion version = ProtocolVersion.MINECRAFT_1_18_2;
    for (ArgumentIdentifier identifier : ArgumentPropertyRegistry.identifiers()) {
      ByteBuf buf = Unpooled.buffer();
      ArgumentPropertyRegistry.writeIdentifier(buf, identifier, version);
      assertEquals(identifier, ArgumentPropertyRegistry.readIdentifier(buf, version));
    }
  }

  @Test
  void registryHasNoIdCollisions() {
    assertTrue(ArgumentPropertyRegistry.indexCollisions.isEmpty(),
        "Unexpected positive-id collisions in registry: "
            + ArgumentPropertyRegistry.indexCollisions);
    // Negative ids are "removed in this version" sentinels and are expected to overlap; they must
    // not be reported as collisions.
    Map<ProtocolVersion, Int2ObjectMap<ArgumentIdentifier>> index =
        ArgumentPropertyRegistry.buildIndex(ArgumentPropertyRegistry.identifiers(),
            new ArrayList<>());
    assertNotNull(index.get(ProtocolVersion.MINECRAFT_1_19_3).get(-1));
  }

  private static void assertResolvesTo(ArgumentIdentifier expected,
      List<ArgumentIdentifier> identifiers, ProtocolVersion version, int id) {
    List<String> collisions = new ArrayList<>();
    Map<ProtocolVersion, Int2ObjectMap<ArgumentIdentifier>> index =
        ArgumentPropertyRegistry.buildIndex(identifiers, collisions);

    assertEquals(expected, index.get(version).get(id));
    assertTrue(collisions.size() > 0, "expected a collision to be reported");
  }
}