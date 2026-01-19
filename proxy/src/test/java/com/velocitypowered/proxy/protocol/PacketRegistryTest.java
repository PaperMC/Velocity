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

package com.velocitypowered.proxy.protocol;

import static com.google.common.collect.Iterables.getLast;
import static com.velocitypowered.api.network.ProtocolVersion.MAXIMUM_VERSION;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_11;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_15;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import com.velocitypowered.proxy.protocol.registry.MultiVersionPacketRegistry;
import com.velocitypowered.proxy.protocol.registry.MultiVersionPacketRegistry.VersionRange;
import com.velocitypowered.proxy.protocol.registry.SimplePacketRegistry;
import org.junit.jupiter.api.Test;

class PacketRegistryTest {

  /**
   * Sets up a multi-version registry with HandshakePacket mapped to different IDs across version
   * ranges.
   */
  private MultiVersionPacketRegistry setupRegistry() {
    return MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_11, 0x01),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_14_4, 0x00),
            VersionRange.of(MINECRAFT_1_15, MAXIMUM_VERSION, 0x00))
        .build();
  }

  // ==================== Basic Functionality Tests ====================

  @Test
  void packetRegistryWorks() {
    MultiVersionPacketRegistry registry = setupRegistry();
    PacketCodec<?> packet = registry.forVersion(MINECRAFT_1_12).getCodec(0);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(HandshakePacket.Codec.class, packet.getClass(), "Registry returned wrong class");

    assertEquals(0, registry.forVersion(MINECRAFT_1_12).getPacketId(HandshakePacket.DEFAULT),
        "Registry did not return the correct packet ID");
  }

  @Test
  void packetRegistryLinkingWorks() {
    MultiVersionPacketRegistry registry = setupRegistry();
    PacketCodec<?> packet = registry.forVersion(MINECRAFT_1_12_1).getCodec(0);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(HandshakePacket.Codec.class, packet.getClass(), "Registry returned wrong class");
    HandshakePacket handshakePacket = HandshakePacket.DEFAULT;
    assertEquals(0, registry.forVersion(MINECRAFT_1_12_1).getPacketId(handshakePacket),
        "Registry did not return the correct packet ID");
    assertEquals(0, registry.forVersion(MINECRAFT_1_14_2).getPacketId(handshakePacket),
        "Registry did not return the correct packet ID");
    assertEquals(1, registry.forVersion(MINECRAFT_1_11).getPacketId(handshakePacket),
        "Registry did not return the correct packet ID");
    assertNull(registry.forVersion(MINECRAFT_1_14_2).getCodec(0x01),
        "Registry should return a null");
    // 1.16_2 is >= 1.15, so it should have codec for 0x00 from the last range
    assertNotNull(registry.forVersion(MINECRAFT_1_16_2).getCodec(0),
        "1.16_2 should have codec for 0x00 from 1.15+ range");
  }

  @Test
  void registrySuppliesCorrectPacketsByProtocol() {
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x00),
            VersionRange.of(MINECRAFT_1_12_1, MINECRAFT_1_12_2, 0x01),
            VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_14_4, 0x02))
        .build();
    assertNotNull(registry.forVersion(MINECRAFT_1_12).getCodec(0x00));
    assertEquals(HandshakePacket.Codec.class,
        registry.forVersion(MINECRAFT_1_12).getCodec(0x00).getClass());
    assertNotNull(registry.forVersion(MINECRAFT_1_12_1).getCodec(0x01));
    assertEquals(HandshakePacket.Codec.class,
        registry.forVersion(MINECRAFT_1_12_1).getCodec(0x01).getClass());
    assertNotNull(registry.forVersion(MINECRAFT_1_12_2).getCodec(0x01));
    assertEquals(HandshakePacket.Codec.class,
        registry.forVersion(MINECRAFT_1_12_2).getCodec(0x01).getClass());
    assertNotNull(registry.forVersion(MINECRAFT_1_13).getCodec(0x02));
    assertEquals(HandshakePacket.Codec.class,
        registry.forVersion(MINECRAFT_1_13).getCodec(0x02).getClass());
    assertNotNull(registry.forVersion(MINECRAFT_1_14_2).getCodec(0x02));
    assertEquals(HandshakePacket.Codec.class,
        registry.forVersion(MINECRAFT_1_14_2).getCodec(0x02).getClass());
  }

  // ==================== Error Handling Tests ====================

  @Test
  void failOnNoMappings() {
    assertThrows(IllegalArgumentException.class,
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .build()
            .forVersion(ProtocolVersion.UNKNOWN));
  }

  @Test
  void failOnWrongVersionOrder() {
    // End version before start version
    assertThrows(IllegalArgumentException.class,
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_13, 0x01),
                VersionRange.of(MINECRAFT_1_8, 0x00))
            .build());
  }

  @Test
  void failOnInvalidRangeStartGreaterThanEnd() {
    // Single VersionRange with start > end
    assertThrows(IllegalArgumentException.class,
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_13, MINECRAFT_1_8, 0x01))
            .build());
  }

  @Test
  void failOnDuplicateVersionRanges() {
    // Two ranges covering the same version
    assertThrows(IllegalArgumentException.class,
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_13, 0x01),
                VersionRange.of(MINECRAFT_1_13, 0x01))
            .build());
  }

  @Test
  void failOnOverlappingVersionRanges() {
    // Same packet registered twice with overlapping versions should fail
    assertThrows(IllegalArgumentException.class,
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_14, 0x01))
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_16, 0x00))
            .build());
  }

  @Test
  void failOnDuplicatePacketClass() {
    // Same packet class registered twice for overlapping versions
    assertThrows(IllegalArgumentException.class,
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_14, 0x00))
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_12, 0x01))
            .build());
  }

  @Test
  void allowDifferentPacketsWithSameIdInDifferentVersions() {
    // Different packets can have the same ID in different version ranges
    assertDoesNotThrow(
        () -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
            .register(HandshakePacket.class, new HandshakePacket.Codec(),
                VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_12, 0x00))
            .register(StatusPingPacket.class, new StatusPingPacket.Codec(),
                VersionRange.of(MINECRAFT_1_13, MAXIMUM_VERSION, 0x00))
            .build());
  }

  @Test
  void failOnInvalidPacketIdLookup() {
    MultiVersionPacketRegistry registry = setupRegistry();
    // Try to get packet ID for a packet type that doesn't exist in the registry
    assertThrows(IllegalArgumentException.class,
        () -> registry.forVersion(MINECRAFT_1_12).getPacketId(new StatusPingPacket(0L)));
  }

  // ==================== Edge Cases and Boundaries ====================

  @Test
  void shouldNotFailWhenRegisterLatestProtocolVersion() {
    assertDoesNotThrow(() -> MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_8, MINECRAFT_1_14_4, 0x00),
            VersionRange.of(getLast(ProtocolVersion.SUPPORTED_VERSIONS), 0x01))
        .build());
  }

  @Test
  void versionBoundaryTransition() {
    // Test that version boundaries are handled correctly
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_12, MINECRAFT_1_12, 0x00),
            VersionRange.of(MINECRAFT_1_13, MAXIMUM_VERSION, 0x01))
        .build();

    // 1.12 should have ID 0x00
    assertEquals(0x00, registry.forVersion(MINECRAFT_1_12).getPacketId(HandshakePacket.DEFAULT));
    // 1.13 should have ID 0x01
    assertEquals(0x01, registry.forVersion(MINECRAFT_1_13).getPacketId(HandshakePacket.DEFAULT));
  }

  @Test
  void multiplePacketsInSingleRegistry() {
    // Test registry with multiple packet types
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_12, 0x00))
        .register(StatusPingPacket.class, new StatusPingPacket.Codec(),
            VersionRange.of(MINECRAFT_1_12, 0x01))
        .build();

    assertEquals(0x00, registry.forVersion(MINECRAFT_1_12).getPacketId(HandshakePacket.DEFAULT));
    assertEquals(0x01, registry.forVersion(MINECRAFT_1_12).getPacketId(new StatusPingPacket(0L)));
    assertNotNull(registry.forVersion(MINECRAFT_1_12).getCodec(0x00));
    assertNotNull(registry.forVersion(MINECRAFT_1_12).getCodec(0x01));
  }

  // ==================== Encode-Only Tests ====================

  @Test
  void encodeOnlyPacketsCanBeEncoded() {
    // Encode-only packets should be decodable by class but not by ID
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.encodeOnly(MINECRAFT_1_12, 0x00))
        .build();

    // Should be able to get codec by class
    assertNotNull(registry.forVersion(MINECRAFT_1_12).getCodec(HandshakePacket.class));
    // Should NOT be able to get codec by ID
    assertNull(registry.forVersion(MINECRAFT_1_12).getCodec(0x00));
  }

  @Test
  void encodeOnlyPacketMultipleVersions() {
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.encodeOnly(MINECRAFT_1_8, MINECRAFT_1_12, 0x00),
            VersionRange.of(MINECRAFT_1_13, 0x01))
        .build();

    // 1.8-1.12 should be encode-only (no decode by ID)
    assertNull(registry.forVersion(MINECRAFT_1_12).getCodec(0x00));
    assertNotNull(registry.forVersion(MINECRAFT_1_12).getCodec(HandshakePacket.class));

    // 1.13+ should be decodable by ID
    assertNotNull(registry.forVersion(MINECRAFT_1_13).getCodec(0x01));
  }

  // ==================== Codec Retrieval Tests ====================

  @Test
  void getCodecByClassWorks() {
    MultiVersionPacketRegistry registry = setupRegistry();
    PacketCodec<HandshakePacket> codec = registry.forVersion(MINECRAFT_1_12)
        .getCodec(HandshakePacket.class);
    assertNotNull(codec, "Should be able to retrieve codec by class");
    assertEquals(HandshakePacket.Codec.class, codec.getClass());
  }

  @Test
  void getCodecByClassReturnsNullForUnregisteredPacket() {
    MultiVersionPacketRegistry registry = setupRegistry();
    PacketCodec<StatusPingPacket> codec = registry.forVersion(MINECRAFT_1_12)
        .getCodec(StatusPingPacket.class);
    assertNull(codec, "Should return null for unregistered packet class");
  }

  @Test
  void getCodecByIdReturnsNullForInvalidId() {
    MultiVersionPacketRegistry registry = setupRegistry();
    PacketCodec<?> codec = registry.forVersion(MINECRAFT_1_12).getCodec(0xFF);
    assertNull(codec, "Should return null for invalid packet ID");
  }

  // ==================== CanDecodePacket Tests ====================

  @Test
  void canDecodePacketReturnsTrue() {
    MultiVersionPacketRegistry registry = setupRegistry();
    assertTrue(registry.forVersion(MINECRAFT_1_12).canDecodePacket(HandshakePacket.DEFAULT),
        "Should be able to decode HandshakePacket in 1.12");
  }

  @Test
  void canDecodePacketReturnsFalse() {
    MultiVersionPacketRegistry registry = setupRegistry();
    assertFalse(registry.forVersion(MINECRAFT_1_12).canDecodePacket(new StatusPingPacket(0L)),
        "Should not be able to decode StatusPingPacket in 1.12");
  }

  @Test
  void encodeOnlyPacketCanBeIdentified() {
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.encodeOnly(MINECRAFT_1_12, 0x00))
        .build();

    // Encode-only packets can still be identified by class for encoding purposes
    assertTrue(registry.forVersion(MINECRAFT_1_12).canDecodePacket(HandshakePacket.DEFAULT),
        "Encode-only packets can be identified for encoding");
    // But cannot be decoded by ID (not in packetIdToCodec)
    assertNull(registry.forVersion(MINECRAFT_1_12).getCodec(0x00),
        "Encode-only packets cannot be decoded from ID");
  }

  // ==================== SimplePacketRegistry Tests ====================

  @Test
  void simplePacketRegistryWorks() {
    SimplePacketRegistry registry = new SimplePacketRegistry(ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(0x00, HandshakePacket.class, new HandshakePacket.Codec());

    PacketCodec<?> codec = registry.getCodec(0x00);
    assertNotNull(codec, "Should retrieve codec by ID");
    assertEquals(HandshakePacket.Codec.class, codec.getClass());
  }

  @Test
  void simplePacketRegistryCodecByClass() {
    SimplePacketRegistry registry = new SimplePacketRegistry(ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(0x00, HandshakePacket.class, new HandshakePacket.Codec());

    PacketCodec<HandshakePacket> codec = registry.getCodec(HandshakePacket.class);
    assertNotNull(codec, "Should retrieve codec by class");
    assertEquals(HandshakePacket.Codec.class, codec.getClass());
  }

  @Test
  void simplePacketRegistryGetPacketId() {
    SimplePacketRegistry registry = new SimplePacketRegistry(ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(0x42, HandshakePacket.class, new HandshakePacket.Codec());

    int id = registry.getPacketId(HandshakePacket.DEFAULT);
    assertEquals(0x42, id, "Should return correct packet ID");
  }

  @Test
  void simplePacketRegistryFailsOnUnregisteredPacket() {
    SimplePacketRegistry registry = new SimplePacketRegistry(ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(0x00, HandshakePacket.class, new HandshakePacket.Codec());

    assertThrows(IllegalArgumentException.class,
        () -> registry.getPacketId(new StatusPingPacket(0L)),
        "Should fail when packet is not registered");
  }

  @Test
  void simplePacketRegistryCanDecodePacket() {
    SimplePacketRegistry registry = new SimplePacketRegistry(ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(0x00, HandshakePacket.class, new HandshakePacket.Codec());

    assertTrue(registry.canDecodePacket(HandshakePacket.DEFAULT),
        "Should be able to decode registered packet");
    assertFalse(registry.canDecodePacket(new StatusPingPacket(0L)),
        "Should not be able to decode unregistered packet");
  }

  // ==================== Direction Tests ====================

  @Test
  void registriesWithDifferentDirections() {
    // CLIENTBOUND registry
    MultiVersionPacketRegistry clientbound = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_12, 0x00))
        .build();

    // SERVERBOUND registry with different ID
    MultiVersionPacketRegistry serverbound = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.SERVERBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_12, 0x01))
        .build();

    assertEquals(0x00, clientbound.forVersion(MINECRAFT_1_12).getPacketId(HandshakePacket.DEFAULT));
    assertEquals(0x01, serverbound.forVersion(MINECRAFT_1_12).getPacketId(HandshakePacket.DEFAULT));
  }

  // ==================== Wide Version Range Tests ====================

  @Test
  void wideVersionRangeSpanningManyVersions() {
    // Register packet across many versions with same ID
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_8, MAXIMUM_VERSION, 0x00))
        .build();

    // Should work for all supported versions in range
    assertEquals(0x00, registry.forVersion(MINECRAFT_1_8).getPacketId(HandshakePacket.DEFAULT));
    assertEquals(0x00, registry.forVersion(MINECRAFT_1_12).getPacketId(HandshakePacket.DEFAULT));
    assertEquals(0x00, registry.forVersion(MINECRAFT_1_16_2).getPacketId(HandshakePacket.DEFAULT));
  }

  @Test
  void nullEndVersionRangeHandling() {
    // Test with null end version (should extend to MAXIMUM_VERSION)
    MultiVersionPacketRegistry registry = MultiVersionPacketRegistry.builder(ProtocolUtils.Direction.CLIENTBOUND)
        .register(HandshakePacket.class, new HandshakePacket.Codec(),
            VersionRange.of(MINECRAFT_1_15, 0x00))
        .build();

    assertEquals(0x00, registry.forVersion(MINECRAFT_1_15).getPacketId(HandshakePacket.DEFAULT));
    assertEquals(0x00, registry.forVersion(MINECRAFT_1_16_2).getPacketId(HandshakePacket.DEFAULT));
  }
}