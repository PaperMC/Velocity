package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LoginPluginMessage implements MinecraftPacket {

  private int id;
  private @Nullable String channel;
  private ByteBuf data = Unpooled.EMPTY_BUFFER;

  public LoginPluginMessage() {

  }

  public LoginPluginMessage(int id, @Nullable String channel, ByteBuf data) {
    this.id = id;
    this.channel = channel;
    this.data = data;
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

  public ByteBuf getData() {
    return data;
  }

  @Override
  public String toString() {
    return "LoginPluginMessage{"
        + "id=" + id
        + ", channel='" + channel + '\''
        + ", data=" + data
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    this.id = ProtocolUtils.readVarInt(buf);
    this.channel = ProtocolUtils.readString(buf);
    if (buf.isReadable()) {
      this.data = buf.readSlice(buf.readableBytes());
    } else {
      this.data = Unpooled.EMPTY_BUFFER;
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, id);
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified!");
    }
    ProtocolUtils.writeString(buf, channel);
    buf.writeBytes(data);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
