package com.velocitypowered.proxy.network.registry.state;

import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundEncryptionRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundServerLoginSuccessPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundEncryptionResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundServerLoginPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundTabCompleteRequestPacket;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryBuilder;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;

public class Version172To176 {
  private Version172To176() {
    throw new AssertionError();
  }

  public static final PacketRegistryMap SERVERBOUND_LOGIN = new PacketRegistryBuilder()
      .dense()
      .register(0x00, ServerboundServerLoginPacket.class, ServerboundServerLoginPacket.ENCODER,
          ServerboundServerLoginPacket.DECODER)
      .register(0x01, ServerboundEncryptionResponsePacket.class,
          ServerboundEncryptionResponsePacket.ENCODER, ServerboundEncryptionResponsePacket.DECODER)
      .build();

  public static final PacketRegistryMap CLIENTBOUND_LOGIN = new PacketRegistryBuilder()
      .dense()
      .register(0x00, ClientboundDisconnectPacket.class, ClientboundDisconnectPacket.ENCODER,
          ClientboundDisconnectPacket.DECODER)
      .register(0x01, ClientboundEncryptionRequestPacket.class,
          ClientboundEncryptionRequestPacket.ENCODER, ClientboundEncryptionRequestPacket.DECODER)
      .register(0x02, ClientboundServerLoginSuccessPacket.class,
          ClientboundServerLoginSuccessPacket.ENCODER, ClientboundServerLoginSuccessPacket.DECODER)
      .build();

  public static final PacketRegistryMap SERVERBOUND_PLAY = new PacketRegistryBuilder()
      .register(0x14, ServerboundTabCompleteRequestPacket.class,
          ServerboundTabCompleteRequestPacket.ENCODER, ServerboundTabCompleteRequestPacket.DECODER)
      .build();
}
