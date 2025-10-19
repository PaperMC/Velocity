/*
 * Copyright (C) 2018-2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;

public final class DialogShowPacket implements MinecraftPacket {

  private final StateRegistry state;
  private final int id;
  private final BinaryTag nbt;

  public DialogShowPacket(StateRegistry state, int id, BinaryTag nbt) {
    this.state = state;
    this.id = id;
    this.nbt = nbt;
  }

  public StateRegistry state() {
    return state;
  }

  public int id() {
    return id;
  }

  public BinaryTag nbt() {
    return nbt;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<DialogShowPacket> {
    private final StateRegistry state;

    public Codec(StateRegistry state) {
      this.state = state;
    }

    @Override
    public DialogShowPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                    ProtocolVersion protocolVersion) {
      int id = state == StateRegistry.CONFIG ? 0 : ProtocolUtils.readVarInt(buf);
      BinaryTag nbt = null;
      if (id == 0) {
        nbt = ProtocolUtils.readBinaryTag(buf, protocolVersion, BinaryTagIO.reader());
      }
      return new DialogShowPacket(state, id, nbt);
    }

    @Override
    public void encode(DialogShowPacket packet, ByteBuf buf, ProtocolUtils.Direction direction,
                       ProtocolVersion protocolVersion) {
      if (packet.state == StateRegistry.CONFIG) {
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, packet.nbt);
      } else {
        ProtocolUtils.writeVarInt(buf, packet.id);
        if (packet.id == 0) {
          ProtocolUtils.writeBinaryTag(buf, protocolVersion, packet.nbt);
        }
      }
    }
  }
}
