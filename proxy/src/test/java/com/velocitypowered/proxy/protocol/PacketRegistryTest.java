package com.velocitypowered.proxy.protocol;

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
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.StatusPing;

import org.junit.jupiter.api.Test;

class PacketRegistryTest {

  private StateRegistry.PacketRegistry setupRegistry() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(Handshake.class, Handshake::new,
        new StateRegistry.PacketMapping(0x01, MINECRAFT_1_8, false),
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_12, false));
    return registry;
  }

  @Test
  void packetRegistryWorks() {
    StateRegistry.PacketRegistry registry = setupRegistry();
    MinecraftPacket packet = registry.getProtocolRegistry(MINECRAFT_1_12).createPacket(0);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(Handshake.class, packet.getClass(), "Registry returned wrong class");

    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_12).getPacketId(packet),
        "Registry did not return the correct packet ID");
  }

  @Test
  void packetRegistryLinkingWorks() {
    StateRegistry.PacketRegistry registry = setupRegistry();
    MinecraftPacket packet = registry.getProtocolRegistry(MINECRAFT_1_12_1).createPacket(0);
    assertNotNull(packet, "Packet was not found in registry");
    assertEquals(Handshake.class, packet.getClass(), "Registry returned wrong class");
    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_12_1).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertEquals(0, registry.getProtocolRegistry(MINECRAFT_1_14_2).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertEquals(1, registry.getProtocolRegistry(MINECRAFT_1_11).getPacketId(packet),
        "Registry did not return the correct packet ID");
    assertNull(registry.getProtocolRegistry(MINECRAFT_1_14_2).createPacket(0x01),
        "Registry should return a null");
  }

  @Test
  void failOnNoMappings() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND);
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(Handshake.class, Handshake::new));
    assertThrows(IllegalArgumentException.class,
        () -> registry.getProtocolRegistry(ProtocolVersion.UNKNOWN).getPacketId(new Handshake()));
  }

  @Test
  void failOnWrongOrder() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND);
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(Handshake.class, Handshake::new,
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_13, false),
            new StateRegistry.PacketMapping(0x00, MINECRAFT_1_8, false)));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(Handshake.class, Handshake::new,
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_13, false),
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_13, false)));
  }

  @Test
  void failOnDuplicate() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(Handshake.class, Handshake::new,
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_8, false));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(Handshake.class, Handshake::new,
            new StateRegistry.PacketMapping(0x01, MINECRAFT_1_12, false)));
    assertThrows(IllegalArgumentException.class,
        () -> registry.register(StatusPing.class, StatusPing::new,
            new StateRegistry.PacketMapping(0x00, MINECRAFT_1_13, false)));
  }

  @Test
  void shouldNotFailWhenRegisterLatestProtocolVersion() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND);
    assertDoesNotThrow(() -> registry.register(Handshake.class, Handshake::new,
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_8, false),
        new StateRegistry.PacketMapping(0x01, getLast(ProtocolVersion.SUPPORTED_VERSIONS),
            false)));
  }

  @Test
  void registrySuppliesCorrectPacketsByProtocol() {
    StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(
        ProtocolUtils.Direction.CLIENTBOUND);
    registry.register(Handshake.class, Handshake::new,
        new StateRegistry.PacketMapping(0x00, MINECRAFT_1_12, false),
        new StateRegistry.PacketMapping(0x01, MINECRAFT_1_12_1, false),
        new StateRegistry.PacketMapping(0x02, MINECRAFT_1_13, false));
    assertEquals(Handshake.class,
        registry.getProtocolRegistry(MINECRAFT_1_12).createPacket(0x00).getClass());
    assertEquals(Handshake.class,
        registry.getProtocolRegistry(MINECRAFT_1_12_1).createPacket(0x01).getClass());
    assertEquals(Handshake.class,
        registry.getProtocolRegistry(MINECRAFT_1_12_2).createPacket(0x01).getClass());
    assertEquals(Handshake.class,
        registry.getProtocolRegistry(MINECRAFT_1_13).createPacket(0x02).getClass());
    assertEquals(Handshake.class,
        registry.getProtocolRegistry(MINECRAFT_1_14_2).createPacket(0x02).getClass());
  }
}