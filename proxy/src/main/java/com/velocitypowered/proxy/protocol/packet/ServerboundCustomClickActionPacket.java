/*
 * Copyright (C) 2018-2026 Velocity Contributors
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
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.EndBinaryTag;

public class ServerboundCustomClickActionPacket implements MinecraftPacket {

  private static final int MAX_TAG_SIZE = 65536;
  private static final long MAX_TAG_BYTES = 32768L;

  private Key id;
  private BinaryTag payload;

  public ServerboundCustomClickActionPacket() {
  }

  public ServerboundCustomClickActionPacket(Key id) {
    this(id, EndBinaryTag.endBinaryTag());
  }

  public ServerboundCustomClickActionPacket(Key id, BinaryTag payload) {
    this.id = id;
    this.payload = payload;
  }

  @Override
  public void decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    this.id = ProtocolUtils.readKey(buf);
    int size = ProtocolUtils.readVarInt(buf);
    if (size > MAX_TAG_SIZE) {
      throw new DecoderException(
          "Buffer size " + size + " is larger than allowed limit of " + MAX_TAG_SIZE);
    }
    ByteBuf slice = buf.readSlice(size);
    this.payload = ProtocolUtils.readBinaryTag(slice, protocolVersion, BinaryTagIO.reader(MAX_TAG_BYTES));
  }

  @Override
  public void encode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeKey(buf, this.id);
    ByteBuf tempBuf = buf.alloc().buffer();
    try {
      ProtocolUtils.writeBinaryTag(tempBuf, protocolVersion, this.payload);
      ProtocolUtils.writeVarInt(buf, tempBuf.readableBytes());
      buf.writeBytes(tempBuf);
    } finally {
      tempBuf.release();
    }
  }

  @Override
  public int decodeExpectedMaxLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    return ProtocolUtils.DEFAULT_MAX_STRING_BYTES + ProtocolUtils.varIntBytes(MAX_TAG_SIZE) + MAX_TAG_SIZE;
  }

  @Override
  public int decodeExpectedMinLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    return 1 + 0 + 1 + 0;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public Key id() {
    return id;
  }

  public BinaryTag payload() {
    return payload;
  }

  @Override
  public String toString() {
    return "ServerboundCustomClickActionPacket{"
      + "id=" + id
      + ", payload=" + payload
      + "}";
  }
}
