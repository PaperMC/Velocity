package com.velocitypowered.proxy.protocol;

import com.velocitypowered.proxy.protocol.packets.Handshake;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PacketRegistryTest {
    private StateRegistry.PacketRegistry setupRegistry() {
        StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(ProtocolConstants.Direction.TO_CLIENT, StateRegistry.HANDSHAKE);
        registry.register(Handshake.class, Handshake::new, new StateRegistry.PacketMapping(0x00, 1));
        return registry;
    }

    @Test
    void packetRegistryWorks() {
        StateRegistry.PacketRegistry registry = setupRegistry();
        MinecraftPacket packet = registry.createPacket(0, 1);
        assertNotNull(packet, "Packet was not found in registry");
        assertEquals(Handshake.class, packet.getClass(), "Registry returned wrong class");

        assertEquals(0, registry.getId(packet, 1), "Registry did not return the correct packet ID");
    }

    @Test
    void packetRegistryRevertsToBestOldVersion() {
        StateRegistry.PacketRegistry registry = setupRegistry();
        MinecraftPacket packet = registry.createPacket(0, 2);
        assertNotNull(packet, "Packet was not found in registry");
        assertEquals(Handshake.class, packet.getClass(), "Registry returned wrong class");

        assertEquals(0, registry.getId(packet, 2), "Registry did not return the correct packet ID");
    }

    @Test
    void packetRegistryDoesntProvideNewPacketsForOld() {
        StateRegistry.PacketRegistry registry = setupRegistry();
        assertNull(registry.createPacket(0, 0), "Packet was found in registry despite being too new");

        assertThrows(IllegalArgumentException.class, () -> registry.getId(new Handshake(), 0), "Registry provided new packets for an old protocol version");
    }

    @Test
    void failOnNoMappings() {
        StateRegistry.PacketRegistry registry = new StateRegistry.PacketRegistry(ProtocolConstants.Direction.TO_CLIENT, StateRegistry.HANDSHAKE);
        assertThrows(IllegalArgumentException.class, () -> registry.register(Handshake.class, Handshake::new));
    }
}