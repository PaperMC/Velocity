package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.protocol.util.PluginMessageUtil.transformLegacyToModernChannel;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PluginMessagePacket extends DeferredByteBufHolder implements Packet {

  private @Nullable String channel;

  public PluginMessagePacket() {
    super(null);
  }

  public PluginMessagePacket(String channel,
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
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
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
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
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
  public PluginMessagePacket copy() {
    return (PluginMessagePacket) super.copy();
  }

  @Override
  public PluginMessagePacket duplicate() {
    return (PluginMessagePacket) super.duplicate();
  }

  @Override
  public PluginMessagePacket retainedDuplicate() {
    return (PluginMessagePacket) super.retainedDuplicate();
  }

  @Override
  public PluginMessagePacket replace(ByteBuf content) {
    return (PluginMessagePacket) super.replace(content);
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
}
