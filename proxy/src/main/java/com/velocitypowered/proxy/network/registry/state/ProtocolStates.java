package com.velocitypowered.proxy.network.registry.state;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusRequestPacket;
import com.velocitypowered.proxy.network.registry.packet.EmptyPacketRegistryMap;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryBuilder;
import com.velocitypowered.proxy.network.registry.protocol.ProtocolRegistry;
import com.velocitypowered.proxy.network.registry.protocol.SimpleProtocolRegistry;
import com.velocitypowered.proxy.network.registry.protocol.VersionSpecificProtocolRegistry;

public class ProtocolStates {
  public static final ProtocolRegistry HANDSHAKE;
  public static final ProtocolRegistry STATUS;
  public static final ProtocolRegistry LOGIN;

  static {
    HANDSHAKE = new SimpleProtocolRegistry(
        new PacketRegistryBuilder()
            .dense()
            .register(0x00, ServerboundHandshakePacket.class, ServerboundHandshakePacket.ENCODER,
              ServerboundHandshakePacket.DECODER)
            .build(),
        EmptyPacketRegistryMap.INSTANCE);

    STATUS = new SimpleProtocolRegistry(
        new PacketRegistryBuilder()
            .dense()
            .register(0x00, ServerboundStatusRequestPacket.class,
                ServerboundStatusRequestPacket.ENCODER,
                ServerboundStatusRequestPacket.DECODER)
            .register(0x01, ServerboundStatusPingPacket.class,
                ServerboundStatusPingPacket.ENCODER,
                ServerboundStatusPingPacket.DECODER)
            .build(),
        new PacketRegistryBuilder()
            .dense()
            .register(0x00, ClientboundStatusResponsePacket.class,
                ClientboundStatusResponsePacket.ENCODER,
                ClientboundStatusResponsePacket.DECODER)
            .register(0x01, ClientboundStatusPingPacket.class,
                ClientboundStatusPingPacket.ENCODER,
                ClientboundStatusPingPacket.DECODER)
            .build());

    LOGIN = new VersionSpecificProtocolRegistry()
        .register(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_8,
            Version172To176.SERVERBOUND_LOGIN, Version172To176.CLIENTBOUND_LOGIN);
  }

  private ProtocolStates() {
    throw new AssertionError();
  }
}
