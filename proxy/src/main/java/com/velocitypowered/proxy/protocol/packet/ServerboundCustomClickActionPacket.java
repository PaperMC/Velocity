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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public class ServerboundCustomClickActionPacket extends DefaultByteBufHolder
    implements MinecraftPacket {

  public ServerboundCustomClickActionPacket(ByteBuf backing) {
    super(backing);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public ServerboundCustomClickActionPacket copy() {
    return (ServerboundCustomClickActionPacket) super.copy();
  }

  @Override
  public ServerboundCustomClickActionPacket duplicate() {
    return (ServerboundCustomClickActionPacket) super.duplicate();
  }

  @Override
  public ServerboundCustomClickActionPacket retainedDuplicate() {
    return (ServerboundCustomClickActionPacket) super.retainedDuplicate();
  }

  @Override
  public ServerboundCustomClickActionPacket replace(ByteBuf content) {
    return (ServerboundCustomClickActionPacket) super.replace(content);
  }

  @Override
  public ServerboundCustomClickActionPacket retain() {
    return (ServerboundCustomClickActionPacket) super.retain();
  }

  @Override
  public ServerboundCustomClickActionPacket retain(int increment) {
    return (ServerboundCustomClickActionPacket) super.retain(increment);
  }

  @Override
  public ServerboundCustomClickActionPacket touch() {
    return (ServerboundCustomClickActionPacket) super.touch();
  }

  @Override
  public ServerboundCustomClickActionPacket touch(Object hint) {
    return (ServerboundCustomClickActionPacket) super.touch(hint);
  }

  public static class Codec implements PacketCodec<ServerboundCustomClickActionPacket> {
    @Override
    public ServerboundCustomClickActionPacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion protocolVersion) {
      return new ServerboundCustomClickActionPacket(buf.readRetainedSlice(buf.readableBytes()));
    }

    @Override
    public void encode(ServerboundCustomClickActionPacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion protocolVersion) {
      buf.writeBytes(packet.content());
    }

    @Override
    public int encodeSizeHint(ServerboundCustomClickActionPacket packet, Direction direction, ProtocolVersion protocolVersion) {
      return packet.content().readableBytes();
    }
  }
}
