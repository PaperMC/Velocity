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
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_11;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_15;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import org.junit.jupiter.api.Test;

class PacketRegistryTest {

  private StateRegistry.PacketRegistry setupRegistry() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, StateRegistry.PLAY);
    registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
      m.readWrite(0x01, MINECRAFT_1_8, null);
      m.readWrite(0x00, MINECRAFT_1_12, null);
      m.readWrite(0x00, MINECRAFT_1_15, MINECRAFT_1_16);
    });
    return registry;
  }

  @Test
  void packetRegistryWorks() {
    StateRegistry.PacketRegistry registry = setupRegistry();
    MinecraftPacket packet = registry.getProtocolRegistry(MINECRAFT_1_12).createPacket(0);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(HandshakePacket.class, packet.getClass(), "Registry returned wrong class");

    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_12).getPacketId(packet),
        "Registry did not return the correct packet ID");
  }

  @Test
  void packetRegistryLinkingWorks() {
    StateRegistry.PacketRegistry registry = setupRegistry();
    MinecraftPacket packet = registry.getProtocolRegistry(MINECRAFT_1_12_1).createPacket(0);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(HandshakePacket.class, packet.getClass(), "Registry returned wrong class");
    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_12_1).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_14_2).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertEquals(1, registry.getProtocolRegistry(MINECRAFT_1_11).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertNull(registry.getProtocolRegistry(MINECRAFT_1_14_2).createPacket(0x01),
        "Registry should return a null");
    assertNull(registry.getProtocolRegistry(MINECRAFT_1_16_2).createPacket(0),
        "Registry should return null");
  }

  @Test
  void failOnNoMappings() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, StateRegistry.PLAY);
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {}));
    assertThrows(IllegalArgumentException.class,
        () -> registry.getProtocolRegistry(ProtocolVersion.UNKNOWN)
                .getPacketId(new HandshakePacket()));
  }

  @Test
  void failOnWrongOrder() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, StateRegistry.PLAY);
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
          m.readWrite(0x01, MINECRAFT_1_13, null);
          m.readWrite(0x00, MINECRAFT_1_8, null);
        }));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
          m.readWrite(0x01, MINECRAFT_1_13, null);
          m.readWrite(0x01, MINECRAFT_1_13, null);
        }));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
          m.readWrite(0x01, MINECRAFT_1_13, MINECRAFT_1_8);
        }));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
          m.readWrite(0x01, MINECRAFT_1_8, MINECRAFT_1_14);
          m.readWrite(0x00, MINECRAFT_1_16, null);
        }));
  }

  @Test
  void failOnDuplicate() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, StateRegistry.PLAY);
    registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
      m.readWrite(0x00, MINECRAFT_1_8, null);
    });
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
          m.readWrite(0x01, MINECRAFT_1_12, null);
        }));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(StatusPingPacket.class, StatusPingPacket::new, m -> {
          m.readWrite(0x00, MINECRAFT_1_13, null);
        }));
  }

  @Test
  void shouldNotFailWhenRegisterLatestProtocolVersion() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, StateRegistry.PLAY);
    assertDoesNotThrow(() -> registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
      m.readWrite(0x00, MINECRAFT_1_8, null);
      m.readWrite(0x01, getLast(ProtocolVersion.SUPPORTED_VERSIONS), null);
    }));
  }

  @Test
  void registrySuppliesCorrectPacketsByProtocol() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND, StateRegistry.PLAY);
    registry.register(HandshakePacket.class, HandshakePacket::new, m -> {
      m.readWrite(0x00, MINECRAFT_1_12, null);
      m.readWrite(0x01, MINECRAFT_1_12_1, null);
      m.readWrite(0x02, MINECRAFT_1_13, null);
    });
    assertEquals(HandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_12).createPacket(0x00).getClass());
    assertEquals(HandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_12_1).createPacket(0x01).getClass());
    assertEquals(HandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_12_2).createPacket(0x01).getClass());
    assertEquals(HandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_13).createPacket(0x02).getClass());
    assertEquals(HandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_14_2).createPacket(0x02).getClass());
  }
}