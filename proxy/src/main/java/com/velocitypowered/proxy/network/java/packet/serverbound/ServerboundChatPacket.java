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
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.java.packet.JavaPacketHandler;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;

public class ServerboundChatPacket implements Packet {
  public static final PacketReader<ServerboundChatPacket> DECODER = (buf, version) -> {
    final String message = ProtocolUtils.readString(buf);
    return new ServerboundChatPacket(message);
  };
  public static final PacketWriter<ServerboundChatPacket> ENCODER = (buf, packet, version) ->
      ProtocolUtils.writeString(buf, packet.message);

  public static final int MAX_MESSAGE_LENGTH = 256;

  private final String message;

  public ServerboundChatPacket(String message) {
    this.message = message;
  }

  @Override
  public boolean handle(JavaPacketHandler handler) {
    return handler.handle(this);
  }

  public String getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("message", this.message)
      .toString();
  }
}
