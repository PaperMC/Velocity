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

package com.velocitypowered.proxy.network.packet;

import static com.velocitypowered.proxy.network.PluginMessageUtil.transformLegacyToModernChannel;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.buffer.TypedDefaultByteBufHolder;
import io.netty.buffer.ByteBuf;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public abstract class AbstractPluginMessagePacket<S extends AbstractPluginMessagePacket<S>> extends TypedDefaultByteBufHolder<S> implements Packet {
  protected static <P extends AbstractPluginMessagePacket<P>> PacketReader<P> decoder(final Factory<P> factory) {
    return (buf, version) -> {
      String channel = ProtocolUtils.readString(buf);
      if (version.gte(ProtocolVersion.MINECRAFT_1_13)) {
        channel = transformLegacyToModernChannel(channel);
      }
      final ByteBuf data;
      if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
        data = buf.readRetainedSlice(buf.readableBytes());
      } else {
        data = ProtocolUtils.readRetainedByteBufSlice17(buf);
      }
      return factory.create(channel, data);
    };
  }

  protected static <P extends AbstractPluginMessagePacket<P>> PacketWriter<P> encoder() {
    return (buf, packet, version) -> {
      if (packet.channel == null) {
        throw new IllegalStateException("Channel is not specified.");
      }
      if (version.gte(ProtocolVersion.MINECRAFT_1_13)) {
        ProtocolUtils.writeString(buf, transformLegacyToModernChannel(packet.channel));
      } else {
        ProtocolUtils.writeString(buf, packet.channel);
      }
      if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
        buf.writeBytes(packet.content());
      } else {
        ProtocolUtils.writeByteBuf17(packet.content(), buf, true); // True for Forge support
      }
    };
  }

  protected final @Nullable String channel;

  protected AbstractPluginMessagePacket(String channel,
                                     @MonotonicNonNull ByteBuf backing) {
    super(backing);
    this.channel = channel;
  }

  public String getChannel() {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
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
    final AbstractPluginMessagePacket<?> that = (AbstractPluginMessagePacket<?>) other;
    return Objects.equals(this.channel, that.channel)
      && super.equals(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.channel, super.hashCode());
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("channel", this.channel)
      .add("data", this.contentToString())
      .toString();
  }

  public interface Factory<P extends AbstractPluginMessagePacket<P>> {
    P create(final String channel, final ByteBuf data);
  }
}
