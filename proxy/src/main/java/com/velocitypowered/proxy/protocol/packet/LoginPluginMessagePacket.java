package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LoginPluginMessagePacket extends DeferredByteBufHolder implements Packet {

  private int id;
  private @Nullable String channel;

  public LoginPluginMessagePacket() {
    super(null);
  }

  public LoginPluginMessagePacket(int id, @Nullable String channel, ByteBuf data) {
    super(data);
    this.id = id;
    this.channel = channel;
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
  public String toString() {
    return "LoginPluginMessage{"
        + "id=" + id
        + ", channel='" + channel + '\''
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    this.id = ProtocolUtils.readVarInt(buf);
    this.channel = ProtocolUtils.readString(buf);
    if (buf.isReadable()) {
      this.replace(buf.readSlice(buf.readableBytes()));
    } else {
      this.replace(Unpooled.EMPTY_BUFFER);
    }
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, id);
    if (channel == null) {
      throw new IllegalStateException("Channel is not specified!");
    }
    ProtocolUtils.writeString(buf, channel);
    buf.writeBytes(content());
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
