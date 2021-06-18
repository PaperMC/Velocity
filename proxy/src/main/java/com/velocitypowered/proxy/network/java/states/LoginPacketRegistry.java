/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.network.java.states;

import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundEncryptionRequestPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundLoginPluginMessagePacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundServerLoginSuccessPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundSetCompressionPacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundEncryptionResponsePacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundLoginPluginResponsePacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundServerLoginPacket;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryBuilder;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;

public class LoginPacketRegistry {
  private LoginPacketRegistry() {
    throw new AssertionError();
  }

  public static final PacketRegistryMap SERVERBOUND_LOGIN_1_7 = baseProtocolServerbound().build();

  public static final PacketRegistryMap CLIENTBOUND_LOGIN_1_7 = baseProtocolClientbound().build();

  public static final PacketRegistryMap CLIENTBOUND_LOGIN_1_8 = baseProtocolClientbound()
      .register(0x03, ClientboundSetCompressionPacket.class,
          ClientboundSetCompressionPacket.DECODER,
          ClientboundSetCompressionPacket.ENCODER)
      .build();

  public static final PacketRegistryMap CLIENTBOUND_LOGIN_1_13 = baseProtocolClientbound()
      .register(0x03, ClientboundSetCompressionPacket.class,
          ClientboundSetCompressionPacket.DECODER,
          ClientboundSetCompressionPacket.ENCODER)
      .register(0x04, ClientboundLoginPluginMessagePacket.class,
          ClientboundLoginPluginMessagePacket.DECODER,
          ClientboundLoginPluginMessagePacket.ENCODER)
      .build();

  public static final PacketRegistryMap SERVERBOUND_LOGIN_1_13 = baseProtocolServerbound()
      .register(0x02, ServerboundLoginPluginResponsePacket.class,
          ServerboundLoginPluginResponsePacket.DECODER,
          ServerboundLoginPluginResponsePacket.ENCODER)
      .build();

  private static PacketRegistryBuilder baseProtocolServerbound() {
    return new PacketRegistryBuilder()
        .dense()
        .register(0x00, ServerboundServerLoginPacket.class, ServerboundServerLoginPacket.DECODER,
            ServerboundServerLoginPacket.ENCODER
        )
        .register(0x01, ServerboundEncryptionResponsePacket.class,
            ServerboundEncryptionResponsePacket.DECODER,
            ServerboundEncryptionResponsePacket.ENCODER);
  }

  private static PacketRegistryBuilder baseProtocolClientbound() {
    return new PacketRegistryBuilder()
        .dense()
        .register(0x00, ClientboundDisconnectPacket.class, ClientboundDisconnectPacket.DECODER,
            ClientboundDisconnectPacket.ENCODER)
        .register(0x01, ClientboundEncryptionRequestPacket.class,
            ClientboundEncryptionRequestPacket.DECODER, ClientboundEncryptionRequestPacket.ENCODER)
        .register(0x02, ClientboundServerLoginSuccessPacket.class,
            ClientboundServerLoginSuccessPacket.DECODER,
            ClientboundServerLoginSuccessPacket.ENCODER);
  }
}
