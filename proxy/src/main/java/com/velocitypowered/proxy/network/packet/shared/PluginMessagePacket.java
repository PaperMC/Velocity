package com.velocitypowered.proxy.network.packet.shared;

import static com.velocitypowered.proxy.network.PluginMessageUtil.transformLegacyToModernChannel;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PluginMessagePacket extends DefaultByteBufHolder implements Packet {
  public static final Decoder<PluginMessagePacket> DECODER = (buf, direction, version) -> {
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
    return new PluginMessagePacket(channel, data);
  };

  private final @Nullable String channel;

  public PluginMessagePacket(String channel,
                             @MonotonicNonNull ByteBuf backing) {
    super(backing);
    this.channel = channel;
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_13)) {
      ProtocolUtils.writeString(buf, transformLegacyToModernChannel(this.channel));
    } else {
      ProtocolUtils.writeString(buf, this.channel);
    }
    if (version.gte(ProtocolVersion.MINECRAFT_1_8)) {
      buf.writeBytes(content());
    } else {
      ProtocolUtils.writeByteBuf17(content(), buf, true); // True for Forge support
    }
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public String getChannel() {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
    }
    return channel;
  }

  @Override
  public PluginMessagePacket replace(ByteBuf content) {
    return new PluginMessagePacket(this.channel, content);
  }

  @Override
  public PluginMessagePacket retain() {
    return (PluginMessagePacket) super.retain();
  }

  @Override
  public PluginMessagePacket retain(int increment) {
    return (PluginMessagePacket) super.retain(increment);
  }

  @Override
  public PluginMessagePacket touch() {
    return (PluginMessagePacket) super.touch();
  }

  @Override
  public PluginMessagePacket touch(Object hint) {
    return (PluginMessagePacket) super.touch(hint);
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (other == null || this.getClass() != other.getClass()) {
      return false;
    }
    final PluginMessagePacket that = (PluginMessagePacket) other;
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
}
