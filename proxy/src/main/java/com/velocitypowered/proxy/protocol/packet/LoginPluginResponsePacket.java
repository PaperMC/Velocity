package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.DeferredByteBufHolder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class LoginPluginResponsePacket extends DeferredByteBufHolder implements Packet {

  private int id;
  private boolean success;

  public LoginPluginResponsePacket() {
    super(Unpooled.EMPTY_BUFFER);
  }

  public LoginPluginResponsePacket(int id, boolean success, @MonotonicNonNull ByteBuf buf) {
    super(buf);
    this.id = id;
    this.success = success;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  @Override
  public String toString() {
    return "LoginPluginResponse{"
        + "id=" + id
        + ", success=" + success
        + ", data=" + super.toString()
        + '}';
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    this.id = ProtocolUtils.readVarInt(buf);
    this.success = buf.readBoolean();
    if (buf.isReadable()) {
      this.replace(buf.readSlice(buf.readableBytes()));
    } else {
      this.replace(Unpooled.EMPTY_BUFFER);
    }
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
}
