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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class ServerboundLoginPluginResponsePacket extends DefaultByteBufHolder implements Packet {
  public static final PacketReader<ServerboundLoginPluginResponsePacket> DECODER = (buf, version) -> {
    final int id = ProtocolUtils.readVarInt(buf);
    final boolean success = buf.readBoolean();
    final ByteBuf data;
    if (buf.isReadable()) {
      data = buf.readSlice(buf.readableBytes());
    } else {
      data = Unpooled.EMPTY_BUFFER;
    }
    return new ServerboundLoginPluginResponsePacket(id, success, data);
  };
  public static final PacketWriter<ServerboundLoginPluginResponsePacket> ENCODER = PacketWriter.deprecatedEncode();

  private final int id;
  private final boolean success;

  public ServerboundLoginPluginResponsePacket(int id, boolean success, @MonotonicNonNull ByteBuf buf) {
    super(buf);
    this.id = id;
    this.success = success;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, id);
    buf.writeBoolean(success);
    buf.writeBytes(content());
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public int getId() {
    return id;
  }

  public boolean isSuccess() {
    return success;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("id", this.id)
      .add("success", this.success)
      .add("data", this.contentToString())
      .toString();
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || this.getClass() != other.getClass()) {
      return false;
    }
    final ServerboundLoginPluginResponsePacket that = (ServerboundLoginPluginResponsePacket) other;
    return this.id == that.id
      && Objects.equals(this.success, that.success)
      && super.equals(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.success, super.hashCode());
  }
}
