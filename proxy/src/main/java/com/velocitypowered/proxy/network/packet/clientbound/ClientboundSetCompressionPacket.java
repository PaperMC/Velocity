package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import io.netty.buffer.ByteBuf;

public class ClientboundSetCompressionPacket implements Packet {
  public static final PacketReader<ClientboundSetCompressionPacket> DECODER = (buf, direction, version) -> {
    final int threshold = ProtocolUtils.readVarInt(buf);
    return new ClientboundSetCompressionPacket(threshold);
  };

  private final int threshold;

  public ClientboundSetCompressionPacket(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, threshold);
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public int getThreshold() {
    return threshold;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
      .add("threshold", this.threshold)
      .toString();
  }
}
