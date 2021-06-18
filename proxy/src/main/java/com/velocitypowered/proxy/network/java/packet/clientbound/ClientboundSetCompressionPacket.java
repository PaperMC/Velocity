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

package com.velocitypowered.proxy.network.java.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;

public class ClientboundSetCompressionPacket implements Packet {
  public static final PacketReader<ClientboundSetCompressionPacket> DECODER = (buf, version) -> {
    final int threshold = ProtocolUtils.readVarInt(buf);
    return new ClientboundSetCompressionPacket(threshold);
  };
  public static final PacketWriter<ClientboundSetCompressionPacket> ENCODER = (buf, packet, version) ->
      ProtocolUtils.writeVarInt(buf, packet.threshold);

  private final int threshold;

  public ClientboundSetCompressionPacket(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public boolean handle(JavaPacketHandler handler) {
    return handler.handle(this);
  }

  public int getThreshold() {
    return threshold;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("threshold", this.threshold)
      .toString();
  }
}
