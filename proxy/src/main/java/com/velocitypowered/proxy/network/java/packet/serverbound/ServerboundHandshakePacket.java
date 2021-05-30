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

package com.velocitypowered.proxy.network.java.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;

public class ServerboundHandshakePacket implements Packet {
  public static final PacketReader<ServerboundHandshakePacket> DECODER = (buf, version) -> {
    int realProtocolVersion = ProtocolUtils.readVarInt(buf);
    final ProtocolVersion protocolVersion = ProtocolVersion.byMinecraftProtocolVersion(realProtocolVersion);
    final String hostname = ProtocolUtils.readString(buf);
    final int port = buf.readUnsignedShort();
    final int nextStatus = ProtocolUtils.readVarInt(buf);
    return new ServerboundHandshakePacket(protocolVersion, hostname, port, nextStatus);
  };
  public static final PacketWriter<ServerboundHandshakePacket> ENCODER = (out, packet, version) -> {
    ProtocolUtils.writeVarInt(out, packet.protocolVersion.protocol());
    ProtocolUtils.writeString(out, packet.serverAddress);
    out.writeShort(packet.port);
    ProtocolUtils.writeVarInt(out, packet.nextStatus);
  };
  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;

  private final ProtocolVersion protocolVersion;
  private final String serverAddress;
  private final int port;
  private final int nextStatus;

  public ServerboundHandshakePacket(final ProtocolVersion protocolVersion, final String hostname, final int port, final int nextStatus) {
    this.protocolVersion = protocolVersion;
    this.serverAddress = hostname;
    this.port = port;
    this.nextStatus = nextStatus;
  }

  @Override
  public boolean handle(JavaPacketHandler handler) {
    return handler.handle(this);
  }

  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
  }

  public String getServerAddress() {
    return serverAddress;
  }

  public int getPort() {
    return port;
  }

  public int getNextStatus() {
    return nextStatus;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("protocolVersion", this.protocolVersion)
      .add("serverAddress", this.serverAddress)
      .add("port", this.port)
      .add("nextStatus", this.nextStatus)
      .toString();
  }
}
