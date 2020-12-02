package com.velocitypowered.proxy.network;

import static com.google.common.collect.Iterables.getLast;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_11;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusPingPacket;

import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class PacketRegistryTest {

  private StateRegistry.PacketRegistry setupRegistry() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        PacketDirection.CLIENTBOUND);
    registry.register(ServerboundHandshakePacket.class, ServerboundHandshakePacket.DECODER,
        new StateRegistry.PacketMapping(0x01, MINECRAFT_1_8, false),
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_12, false));
    return registry;
  }

  @Test
  void packetRegistryWorks() {
    StateRegistry.PacketRegistry registry = setupRegistry();
    Packet packet = registry.getProtocolRegistry(MINECRAFT_1_12).readPacket(0, Unpooled.EMPTY_BUFFER, PacketDirection.SERVERBOUND, MINECRAFT_1_12);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(ServerboundHandshakePacket.class,
        packet.getClass(), "Registry returned wrong class");

    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_12).getPacketId(packet),
        "Registry did not return the correct packet ID");
  }

  @Test
  void packetRegistryLinkingWorks() {
    StateRegistry.PacketRegistry registry = setupRegistry();
    Packet packet = registry.getProtocolRegistry(MINECRAFT_1_12_1).readPacket(0, Unpooled.EMPTY_BUFFER, PacketDirection.SERVERBOUND, MINECRAFT_1_12);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(ServerboundHandshakePacket.class,
        packet.getClass(), "Registry returned wrong class");
    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_12_1).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_14_2).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertEquals(1, registry.getProtocolRegistry(MINECRAFT_1_11).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertNull(registry.getProtocolRegistry(MINECRAFT_1_14_2).readPacket(0x01, Unpooled.EMPTY_BUFFER, PacketDirection.SERVERBOUND, MINECRAFT_1_12),
        "Registry should return a null");
  }

  @Test
  void failOnNoMappings() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        PacketDirection.CLIENTBOUND);
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(ServerboundHandshakePacket.class,
            ServerboundHandshakePacket.DECODER));
    assertThrows(IllegalArgumentException.class,
        () -> registry.getProtocolRegistry(ProtocolVersion.UNKNOWN)
            .getPacketId(new ServerboundHandshakePacket()));
  }

  @Test
  void failOnWrongOrder() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        PacketDirection.CLIENTBOUND);
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(ServerboundHandshakePacket.class,
            ServerboundHandshakePacket.DECODER,
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_13, false),
            new StateRegistry.PacketMapping(0x00, MINECRAFT_1_8, false)));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(ServerboundHandshakePacket.class,
            ServerboundHandshakePacket.DECODER,
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_13, false),
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_13, false)));
  }

  @Test
  void failOnDuplicate() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        PacketDirection.CLIENTBOUND);
    registry.register(ServerboundHandshakePacket.class,
        ServerboundHandshakePacket.DECODER,
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_8, false));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(ServerboundHandshakePacket.class,
            ServerboundHandshakePacket.DECODER,
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_12, false)));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(ServerboundStatusPingPacket.class, ServerboundStatusPingPacket.DECODER,
            new StateRegistry.PacketMapping(0x00, MINECRAFT_1_13, false)));
  }

  @Test
  void shouldNotFailWhenRegisterLatestProtocolVersion() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        PacketDirection.CLIENTBOUND);
    assertDoesNotThrow(() -> registry.register(ServerboundHandshakePacket.class,
        ServerboundHandshakePacket.DECODER,
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_8, false),
        new StateRegistry.PacketMapping(0x01, getLast(ProtocolVersion.SUPPORTED_VERSIONS),
            false)));
  }

  @Test
  void registrySuppliesCorrectPacketsByProtocol() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        PacketDirection.CLIENTBOUND);
    registry.register(ServerboundHandshakePacket.class, ServerboundHandshakePacket.DECODER,
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_12, false),
        new StateRegistry.PacketMapping(0x01, MINECRAFT_1_12_1, false),
        new StateRegistry.PacketMapping(0x02, MINECRAFT_1_13, false));
    assertEquals(ServerboundHandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_12).readPacket(0x00, Unpooled.EMPTY_BUFFER,
            PacketDirection.SERVERBOUND, MINECRAFT_1_12).getClass());
    assertEquals(ServerboundHandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_12_1).readPacket(0x01, Unpooled.EMPTY_BUFFER,
            PacketDirection.SERVERBOUND, MINECRAFT_1_12).getClass());
    assertEquals(ServerboundHandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_12_2).readPacket(0x01, Unpooled.EMPTY_BUFFER,
            PacketDirection.SERVERBOUND, MINECRAFT_1_12).getClass());
    assertEquals(ServerboundHandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_13).readPacket(0x02, Unpooled.EMPTY_BUFFER,
            PacketDirection.SERVERBOUND, MINECRAFT_1_12).getClass());
    assertEquals(ServerboundHandshakePacket.class,
        registry.getProtocolRegistry(MINECRAFT_1_14_2).readPacket(0x02, Unpooled.EMPTY_BUFFER,
            PacketDirection.SERVERBOUND, MINECRAFT_1_12).getClass());
  }
}