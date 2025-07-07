/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.config;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.EndBinaryTag;
import org.jetbrains.annotations.Nullable;

public class CustomClickActionPacket implements MinecraftPacket {

  private Key id;
  private BinaryTag tag;

  public Key getId() {
    return this.id;
  }

  public void setId(Key id) {
    this.id = id;
  }

  public @Nullable BinaryTag getTag() {
    return this.tag;
  }

  public void setTag(@Nullable BinaryTag tag) {
    this.tag = tag;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    id = ProtocolUtils.readKey(buf);
    int expectedSize = ProtocolUtils.readVarInt(buf);
    int start = buf.readerIndex();
    tag = ProtocolUtils.readBinaryTag(buf, protocolVersion, BinaryTagIO.reader());
    if (tag.type() == BinaryTagTypes.END) {
      tag = null;
    }
    if (buf.readerIndex() != start + expectedSize) {
      throw new DecoderException("Expected " + expectedSize + " bytes, got " + buf.readerIndex());
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeKey(buf, id);
    ByteBuf write = buf.alloc().buffer();
    ProtocolUtils.writeBinaryTag(write, protocolVersion, tag != null ? tag : EndBinaryTag.endBinaryTag());
    ProtocolUtils.writeVarInt(buf, write.readableBytes());
    buf.writeBytes(write);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
