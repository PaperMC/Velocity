/*
 * Copyright (C) 2018-2021 Velocity Contributors
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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public final class LoginPluginResponsePacket extends DefaultByteBufHolder
    implements MinecraftPacket {

  private final int id;
  private final boolean success;

  public LoginPluginResponsePacket(int id, boolean success, @MonotonicNonNull ByteBuf buf) {
    super(buf);
    this.id = id;
    this.success = success;
  }

  public int id() {
    return id;
  }

  public boolean success() {
    return success;
  }

  @Override
  public String toString() {
    return "LoginPluginResponse{"
        + "id=" + id
        + ", success=" + success
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<LoginPluginResponsePacket> {
    public static final Codec INSTANCE = new Codec();

    @Override
    public LoginPluginResponsePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
                                             ProtocolVersion version) {
      int id = ProtocolUtils.readVarInt(buf);
      boolean success = buf.readBoolean();
      ByteBuf data;
      if (buf.isReadable()) {
        data = buf.readRetainedSlice(buf.readableBytes());
      } else {
        data = Unpooled.EMPTY_BUFFER;
      }
      return new LoginPluginResponsePacket(id, success, data);
    }

    @Override
    public void encode(LoginPluginResponsePacket packet, ByteBuf buf,
                       ProtocolUtils.Direction direction, ProtocolVersion version) {
      ProtocolUtils.writeVarInt(buf, packet.id);
      buf.writeBoolean(packet.success);
      buf.writeBytes(packet.content());
    }

    public int encodeSizeHint(LoginPluginResponsePacket packet, ProtocolUtils.Direction direction, ProtocolVersion version) {
      return ProtocolUtils.varIntBytes(packet.id) + 1 + packet.content().readableBytes();
    }
  }
}
