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
    this.replace(buf.readRetainedSlice(buf.readableBytes()));
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
    }
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      ProtocolUtils.writeString(buf, transformLegacyToModernChannel(this.channel));
    } else {
      ProtocolUtils.writeString(buf, this.channel);
    }
    buf.writeBytes(content());
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
