package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LoginPluginMessagePacket extends DefaultByteBufHolder implements Packet {

  public static final Decoder<LoginPluginMessagePacket> DECODER = (buf, direction, version) -> {
    final int id = ProtocolUtils.readVarInt(buf);
    final String channel = ProtocolUtils.readString(buf);
    final ByteBuf data;
    if (buf.isReadable()) {
      data = buf.readSlice(buf.readableBytes());
    } else {
      data = Unpooled.EMPTY_BUFFER;
    }
    return new LoginPluginMessagePacket(id, channel, data);
  };

  private final int id;
  private final @Nullable String channel;

  public LoginPluginMessagePacket(int id, @Nullable String channel, ByteBuf data) {
    super(data);
    this.id = id;
    this.channel = channel;
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
    return "LoginPluginMessagePacket{"
      + "id=" + id
      + ", channel='" + channel + '\''
      + ", data=" + super.toString()
      + '}';
  }

  @Override
  public boolean equals(final Object other) {
    if(this == other) return true;
    if(other == null || this.getClass() != other.getClass()) return false;
    final LoginPluginMessagePacket that = (LoginPluginMessagePacket) other;
    return this.id == that.id
        && Objects.equals(this.channel, that.channel)
        && super.equals(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.channel, super.hashCode());
  }
}
