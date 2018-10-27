package com.velocitypowered.proxy.protocol;

import com.velocitypowered.proxy.protocol.packet.Handshake;
import org.junit.jupiter.api.Test;

import static com.velocitypowered.proxy.protocol.ProtocolConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class PacketRegistryTest {
    private StateRegistry.PacketRegistry setupRegistry() {
        StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(ProtocolConstants.Direction.CLIENTBOUND);
        registry.register(Handshake.class, Handshake::new, new StateRegistry.PacketMapping(0x00, MINECRAFT_1_12, false));
        return registry;
    }

    @Test
    void packetRegistryWorks() {
        StateRegistry.PacketRegistry registry = setupRegistry();
        MinecraftPacket packet = registry.getVersion(MINECRAFT_1_12).createPacket(0);
        assertNotNull(packet, "Packet was not found in registry");
        assertEquals(Handshake.class, packet.getClass(), "Registry returned wrong class");

        assertEquals(0, registry.getVersion(MINECRAFT_1_12).getPacketId(packet), "Registry did not return the correct packet ID");
    }

    @Test
    void packetRegistryLinkingWorks() {
        StateRegistry.PacketRegistry registry = setupRegistry();
        MinecraftPacket packet = registry.getVersion(MINECRAFT_1_12_1).createPacket(0);
        assertNotNull(packet, "Packet was not found in registry");
        assertEquals(Handshake.class, packet.getClass(), "Registry returned wrong class");
        assertEquals(0, registry.getVersion(MINECRAFT_1_12_1).getPacketId(packet), "Registry did not return the correct packet ID");
    }

    @Test
    void failOnNoMappings() {
        StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(ProtocolConstants.Direction.CLIENTBOUND);
        assertThrows(IllegalArgumentException.class, () -> registry.register(Handshake.class, Handshake::new));
        assertThrows(IllegalArgumentException.class, () -> registry.getVersion(0).getPacketId(new Handshake()));
    }

    @Test
    void registrySuppliesCorrectPacketsByProtocol() {
        StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(ProtocolConstants.Direction.CLIENTBOUND);
        registry.register(Handshake.class, Handshake::new, new StateRegistry.PacketMapping(0x00, MINECRAFT_1_12, false),
                new StateRegistry.PacketMapping(0x01, MINECRAFT_1_12_1, false));
        assertEquals(Handshake.class, registry.getVersion(MINECRAFT_1_12).createPacket(0x00).getClass());
        assertEquals(Handshake.class, registry.getVersion(MINECRAFT_1_12_1).createPacket(0x01).getClass());
        assertEquals(Handshake.class, registry.getVersion(MINECRAFT_1_12_2).createPacket(0x01).getClass());
    }
}