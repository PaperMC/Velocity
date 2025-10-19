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
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;

public final class LoginPluginMessagePacket extends DefaultByteBufHolder implements MinecraftPacket {

  private final int id;
  private final String channel;

  public LoginPluginMessagePacket(int id, String channel, ByteBuf data) {
    super(data);
    this.id = id;
    this.channel = channel;
  }

  public int getId() {
    return id;
  }

  public String getChannel() {
    return channel;
  }

  @Override
  public String toString() {
    return "LoginPluginMessage{"
        + "id=" + id
        + ", channel='" + channel + '\''
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public static class Codec implements PacketCodec<LoginPluginMessagePacket> {
    @Override
    public LoginPluginMessagePacket decode(ByteBuf buf, ProtocolUtils.Direction direction,
        ProtocolVersion version) {
      int id = ProtocolUtils.readVarInt(buf);
      String channel = ProtocolUtils.readString(buf);
      ByteBuf data;
      if (buf.isReadable()) {
        data = buf.readRetainedSlice(buf.readableBytes());
      } else {
        data = Unpooled.EMPTY_BUFFER;
      }
      return new LoginPluginMessagePacket(id, channel, data);
    }

    @Override
    public void encode(LoginPluginMessagePacket packet, ByteBuf buf,
        ProtocolUtils.Direction direction, ProtocolVersion version) {
      ProtocolUtils.writeVarInt(buf, packet.id);
      ProtocolUtils.writeString(buf, packet.channel);
      buf.writeBytes(packet.content());
    }

    @Override
    public int encodeSizeHint(LoginPluginMessagePacket packet, Direction direction,
        ProtocolVersion version) {
      return ProtocolUtils.varIntBytes(packet.id)
          + ProtocolUtils.stringSizeHint(packet.channel)
          + packet.content().readableBytes();
    }
  }
}
