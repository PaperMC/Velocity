package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;
import static com.velocitypowered.proxy.protocol.util.PluginMessageUtil.transformLegacyToModernChannel;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PluginMessage implements MinecraftPacket {

  private @Nullable String channel;
  private byte[] data = EMPTY_BYTE_ARRAY;

  public String getChannel() {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
    }
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  @Override
  public String toString() {
    return "PluginMessage{"
        + "channel='" + channel + '\''
        + ", data=<removed>"
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.channel = ProtocolUtils.readString(buf);
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      this.channel = transformLegacyToModernChannel(this.channel);
    }
    this.data = new byte[buf.readableBytes()];
    buf.readBytes(data);
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
    buf.writeBytes(data);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
