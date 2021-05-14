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

package com.velocitypowered.proxy.network.packet.serverbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import io.netty.buffer.ByteBuf;
import java.util.Objects;

public class ServerboundServerLoginPacket implements Packet {
  private static final QuietDecoderException EMPTY_USERNAME = new QuietDecoderException("Empty username!");

  public static final PacketReader<ServerboundServerLoginPacket> DECODER = new PacketReader<>() {
    @Override
    public ServerboundServerLoginPacket read(ByteBuf buf, ProtocolVersion version) {
      final String username = ProtocolUtils.readString(buf, 16);
      if (username.isEmpty()) {
        throw EMPTY_USERNAME;
      }
      return new ServerboundServerLoginPacket(username);
    }

    @Override
    public int expectedMaxLength(ByteBuf buf, ProtocolVersion version) {
      // Accommodate the rare (but likely malicious) use of UTF-8 usernames, since it is technically
      // legal on the protocol level.
      return 1 + (16 * 4);
    }
  };

  public static final PacketWriter<ServerboundServerLoginPacket> ENCODER = (buf, packet, version) ->
      ProtocolUtils.writeString(buf, packet.username);

  private final String username;

  public ServerboundServerLoginPacket(String username) {
    this.username = Objects.requireNonNull(username, "username");
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getUsername() {
    return username;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("username", this.username)
      .toString();
  }
}
