package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ClientboundStatusResponsePacket implements Packet {
  public static final PacketReader<ClientboundStatusResponsePacket> DECODER = (buf, version) -> {
    final String status = ProtocolUtils.readString(buf, Short.MAX_VALUE);
    return new ClientboundStatusResponsePacket(status);
  };
  public static final PacketWriter<ClientboundStatusResponsePacket> ENCODER = (buf, packet, version) ->
      ProtocolUtils.writeString(buf, packet.status);

  private final @Nullable CharSequence status;

  public ClientboundStatusResponsePacket(CharSequence status) {
    this.status = status;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    if (status == null) {
      throw new IllegalStateException("Status is not specified");
    }
    ProtocolUtils.writeString(buf, status);
  }

  @Override
  public boolean handle(PacketHandler handler) {
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
    return MoreObjects.toStringHelper(this)
      .add("status", this.status)
      .toString();
  }
}
