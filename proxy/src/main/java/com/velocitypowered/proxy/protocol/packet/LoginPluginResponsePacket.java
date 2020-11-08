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
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class LoginPluginResponsePacket extends DefaultByteBufHolder implements Packet {

  public static final Decoder<LoginPluginResponsePacket> DECODER = (buf, direction, version) -> {
    final int id = ProtocolUtils.readVarInt(buf);
    final boolean success = buf.readBoolean();
    final ByteBuf data;
    if (buf.isReadable()) {
      data = buf.readSlice(buf.readableBytes());
    } else {
      data = Unpooled.EMPTY_BUFFER;
    }
    return new LoginPluginResponsePacket(id, success, data);
  };

  private final int id;
  private final boolean success;

  public LoginPluginResponsePacket(int id, boolean success, @MonotonicNonNull ByteBuf buf) {
    super(buf);
    this.id = id;
    this.success = success;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, id);
    buf.writeBoolean(success);
    buf.writeBytes(content());
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public int getId() {
    return id;
  }

  public boolean isSuccess() {
    return success;
  }

  @Override
  public String toString() {
    return "LoginPluginResponsePacket{"
        + "id=" + id
        + ", success=" + success
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public boolean equals(final Object other) {
    if(this == other) return true;
    if(other == null || this.getClass() != other.getClass()) return false;
    final LoginPluginResponsePacket that = (LoginPluginResponsePacket) other;
    return this.id == that.id
      && Objects.equals(this.success, that.success)
      && super.equals(other);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.id, this.success, super.hashCode());
  }
}
