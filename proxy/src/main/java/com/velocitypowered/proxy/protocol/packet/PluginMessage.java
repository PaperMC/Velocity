package com.velocitypowered.proxy.protocol.packet;

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
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
    return "PluginMessage{" +
        "channel='" + channel + '\'' +
        ", data=" + ByteBufUtil.hexDump(data) +
        '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
    this.channel = ProtocolUtils.readString(buf);
    this.data = new byte[buf.readableBytes()];
    buf.readBytes(data);
  }

  @Override
  public void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified.");
    }
    ProtocolUtils.writeString(buf, channel);
    buf.writeBytes(data);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
