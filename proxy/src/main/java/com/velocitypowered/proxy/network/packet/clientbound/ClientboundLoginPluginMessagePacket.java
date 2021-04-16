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

package com.velocitypowered.proxy.network.packet.clientbound;

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
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundLoginPluginMessagePacket extends DefaultByteBufHolder implements Packet {
  public static final PacketReader<ClientboundLoginPluginMessagePacket> DECODER = (buf, version) -> {
    final int id = ProtocolUtils.readVarInt(buf);
    final String channel = ProtocolUtils.readString(buf);
    final ByteBuf data;
    if (buf.isReadable()) {
      data = buf.readRetainedSlice(buf.readableBytes());
    } else {
      data = Unpooled.EMPTY_BUFFER;
    }
    return new ClientboundLoginPluginMessagePacket(id, channel, data);
  };
  public static final PacketWriter<ClientboundLoginPluginMessagePacket> ENCODER = PacketWriter.deprecatedEncode();

  private final int id;
  private final @Nullable String channel;

  public ClientboundLoginPluginMessagePacket(int id, @Nullable String channel, ByteBuf data) {
    super(data);
    this.id = id;
    this.channel = channel;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, id);
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified!");
    }
    ProtocolUtils.writeString(buf, channel);
    buf.writeBytes(content());
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public int getId() {
    return id;
  }

  public String getChannel() {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified!");
    }
    return channel;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || this.getClass() != other.getClass()) {
      return false;
    }
    final ClientboundLoginPluginMessagePacket that = (ClientboundLoginPluginMessagePacket) other;
    return this.id == that.id
        && Objects.equals(this.channel, that.channel)
        && super.equals(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.channel, super.hashCode());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("id", this.id)
      .add("channel", this.channel)
      .add("data", this.contentToString())
      .toString();
  }
}
