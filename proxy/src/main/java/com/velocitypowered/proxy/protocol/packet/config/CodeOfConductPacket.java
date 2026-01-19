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

package com.velocitypowered.proxy.protocol.packet.config;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;

public class CodeOfConductPacket extends DefaultByteBufHolder implements MinecraftPacket {

  public CodeOfConductPacket(ByteBuf buf) {
    super(buf);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public CodeOfConductPacket copy() {
    return (CodeOfConductPacket) super.copy();
  }

  @Override
  public CodeOfConductPacket duplicate() {
    return (CodeOfConductPacket) super.duplicate();
  }

  @Override
  public CodeOfConductPacket retainedDuplicate() {
    return (CodeOfConductPacket) super.retainedDuplicate();
  }

  @Override
  public CodeOfConductPacket replace(ByteBuf content) {
    return (CodeOfConductPacket) super.replace(content);
  }

  @Override
  public CodeOfConductPacket retain() {
    return (CodeOfConductPacket) super.retain();
  }

  @Override
  public CodeOfConductPacket retain(int increment) {
    return (CodeOfConductPacket) super.retain(increment);
  }

  @Override
  public CodeOfConductPacket touch() {
    return (CodeOfConductPacket) super.touch();
  }

  @Override
  public CodeOfConductPacket touch(Object hint) {
    return (CodeOfConductPacket) super.touch(hint);
  }

  public static class Codec implements PacketCodec<CodeOfConductPacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public CodeOfConductPacket decode(ByteBuf buf, Direction direction, ProtocolVersion protocolVersion) {
      return new CodeOfConductPacket(buf.readRetainedSlice(buf.readableBytes()));
    }

    @Override
    public void encode(CodeOfConductPacket packet, ByteBuf buf, Direction direction,
                       ProtocolVersion protocolVersion) {
      buf.writeBytes(packet.content());
    }

    @Override
    public int encodeSizeHint(CodeOfConductPacket packet, Direction direction,
        ProtocolVersion version) {
      return packet.content().readableBytes();
    }
  }
}
