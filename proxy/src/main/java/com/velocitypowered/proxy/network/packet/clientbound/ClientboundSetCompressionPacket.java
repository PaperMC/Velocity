package com.velocitypowered.proxy.network.packet.clientbound;

import com.google.common.base.MoreObjects;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;

public class ClientboundSetCompressionPacket implements Packet {
  public static final PacketReader<ClientboundSetCompressionPacket> DECODER = (buf, version) -> {
    final int threshold = ProtocolUtils.readVarInt(buf);
    return new ClientboundSetCompressionPacket(threshold);
  };
  public static final PacketWriter<ClientboundSetCompressionPacket> ENCODER = (buf, packet, version) ->
      ProtocolUtils.writeVarInt(buf, packet.threshold);

  private final int threshold;

  public ClientboundSetCompressionPacket(int threshold) {
    this.threshold = threshold;
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
