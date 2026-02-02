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
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import com.velocitypowered.proxy.protocol.StateRegistry;
import io.netty.buffer.ByteBuf;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;

/**
 * Represents the packet sent by the server to the client to display a configuration dialog
 * during the configuration phase in Minecraft 1.21.6+.
 *
 * <p>This packet is only relevant in the CONFIG and PLAY states. If the ID is {@code 0},
 * a dialog is to be shown and the accompanying {@link BinaryTag} contains its data.</p>
 */
public class DialogShowPacket implements MinecraftPacket {

  private final StateRegistry state;
  private int id;
  private BinaryTag nbt;

  public DialogShowPacket(final StateRegistry state) {
    this.state = state;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.id = this.state == StateRegistry.CONFIG ? 0 : ProtocolUtils.readVarInt(buf);
    if (this.id == 0) {
      this.nbt = ProtocolUtils.readBinaryTag(buf, protocolVersion, BinaryTagIO.reader());
    }
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    if (this.state == StateRegistry.CONFIG) {
      ProtocolUtils.writeBinaryTag(buf, protocolVersion, this.nbt);
    } else {
      ProtocolUtils.writeVarInt(buf, this.id);
      if (this.id == 0) {
        ProtocolUtils.writeBinaryTag(buf, protocolVersion, this.nbt);
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
