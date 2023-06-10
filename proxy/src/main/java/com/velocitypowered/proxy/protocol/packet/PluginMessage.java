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

import static com.velocitypowered.proxy.protocol.util.PluginMessageUtil.transformLegacyToModernChannel;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PluginMessage extends DeferredByteBufHolder implements MinecraftPacket {

  private @Nullable String channel;

  public PluginMessage() {
    super(null);
  }

  public PluginMessage(String channel,
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

  public void setChannel(String channel) {
    this.channel = channel;
  }

  @Override
  public String toString() {
    return "PluginMessage{"
        + "channel='" + channel + '\''
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.channel = ProtocolUtils.readString(buf);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      this.channel = transformLegacyToModernChannel(this.channel);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.replace(buf.readRetainedSlice(buf.readableBytes()));
    } else {
      this.replace(ProtocolUtils.readRetainedByteBufSlice17(buf));
    }

  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
    }

    if (refCnt() == 0) {
      throw new IllegalStateException("Plugin message contents for " + this.channel
          + " freed too many times.");
    }

    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      ProtocolUtils.writeString(buf, transformLegacyToModernChannel(this.channel));
    } else {
      ProtocolUtils.writeString(buf, this.channel);
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      buf.writeBytes(content());
    } else {
      ProtocolUtils.writeByteBuf17(content(), buf, true); // True for Forge support
    }

  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public PluginMessage copy() {
    return (PluginMessage) super.copy();
  }

  @Override
  public PluginMessage duplicate() {
    return (PluginMessage) super.duplicate();
  }

  @Override
  public PluginMessage retainedDuplicate() {
    return (PluginMessage) super.retainedDuplicate();
  }

  @Override
  public PluginMessage replace(ByteBuf content) {
    return (PluginMessage) super.replace(content);
  }

  @Override
  public PluginMessage retain() {
    return (PluginMessage) super.retain();
  }

  @Override
  public PluginMessage retain(int increment) {
    return (PluginMessage) super.retain(increment);
  }

  @Override
  public PluginMessage touch() {
    return (PluginMessage) super.touch();
  }

  @Override
  public PluginMessage touch(Object hint) {
    return (PluginMessage) super.touch(hint);
  }
}
