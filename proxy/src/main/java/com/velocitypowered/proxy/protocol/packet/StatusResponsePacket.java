package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class StatusResponsePacket implements Packet {

  public static final Decoder<StatusResponsePacket> DECODER = (buf, direction, version) -> {
    final String status = ProtocolUtils.readString(buf, Short.MAX_VALUE);
    return new StatusResponsePacket(status);
  };

  private final @Nullable CharSequence status;

  public StatusResponsePacket(CharSequence status) {
    this.status = status;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    if (status == null) {
      throw new IllegalStateException("Status is not specified");
    }
    ProtocolUtils.writeString(buf, status);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  public String getStatus() {
    if (status == null) {
      throw new IllegalStateException("Status is not specified");
    }
    return status.toString();
  }

  @Override
  public String toString() {
    return "StatusResponsePacket{"
      + "status='" + status + '\''
      + '}';
  }
}
