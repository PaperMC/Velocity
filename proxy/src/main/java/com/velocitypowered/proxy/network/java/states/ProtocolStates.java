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

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundStatusPingPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundStatusResponsePacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundStatusPingPacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundStatusRequestPacket;
import com.velocitypowered.proxy.network.registry.packet.EmptyPacketRegistryMap;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryBuilder;
import com.velocitypowered.proxy.network.registry.protocol.ProtocolRegistry;
import com.velocitypowered.proxy.network.registry.protocol.SimpleProtocolRegistry;
import com.velocitypowered.proxy.network.registry.protocol.VersionSpecificProtocolRegistry;

public class ProtocolStates {
  public static final ProtocolRegistry HANDSHAKE;
  public static final ProtocolRegistry STATUS;
  public static final ProtocolRegistry LOGIN;
  public static final ProtocolRegistry PLAY;

  static {
    HANDSHAKE = new SimpleProtocolRegistry(
        new PacketRegistryBuilder()
            .register(0x00, ServerboundHandshakePacket.class, ServerboundHandshakePacket.DECODER,
                ServerboundHandshakePacket.ENCODER)
            .build(),
        EmptyPacketRegistryMap.INSTANCE);

    STATUS = new SimpleProtocolRegistry(
        new PacketRegistryBuilder()
            .register(0x00, ServerboundStatusRequestPacket.class,
                ServerboundStatusRequestPacket.DECODER, ServerboundStatusRequestPacket.ENCODER
            )
            .register(0x01, ServerboundStatusPingPacket.class,
                ServerboundStatusPingPacket.DECODER, ServerboundStatusPingPacket.ENCODER
            )
            .build(),
        new PacketRegistryBuilder()
            .register(0x00, ClientboundStatusResponsePacket.class,
                ClientboundStatusResponsePacket.DECODER, ClientboundStatusResponsePacket.ENCODER
            )
            .register(0x01, ClientboundStatusPingPacket.class,
                ClientboundStatusPingPacket.DECODER, ClientboundStatusPingPacket.ENCODER
            )
            .build());

    LOGIN = new VersionSpecificProtocolRegistry()
        .register(ProtocolVersion.MINECRAFT_1_7_2, ProtocolVersion.MINECRAFT_1_7_6,
            LoginPacketRegistry.SERVERBOUND_LOGIN_1_7, LoginPacketRegistry.CLIENTBOUND_LOGIN_1_7)
        .register(ProtocolVersion.MINECRAFT_1_8, ProtocolVersion.MINECRAFT_1_12_2,
            LoginPacketRegistry.SERVERBOUND_LOGIN_1_7, LoginPacketRegistry.CLIENTBOUND_LOGIN_1_8)
        .register(ProtocolVersion.MINECRAFT_1_13, ProtocolVersion.MAXIMUM_VERSION,
            LoginPacketRegistry.SERVERBOUND_LOGIN_1_13, LoginPacketRegistry.CLIENTBOUND_LOGIN_1_13);

    PLAY = PlayPacketRegistry.PLAY;
  }

  private ProtocolStates() {
    throw new AssertionError();
  }
}
