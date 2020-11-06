package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class SetCompressionPacket implements Packet {

  public static final Decoder<SetCompressionPacket> DECODER = (buf, direction, version) -> {
    final int threshold = ProtocolUtils.readVarInt(buf);
    return new SetCompressionPacket(threshold);
  };

  private final int threshold;

  public SetCompressionPacket(int threshold) {
    this.threshold = threshold;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    ProtocolUtils.writeVarInt(buf, threshold);
  }

  public int getThreshold() {
    return threshold;
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public String toString() {
    return "SetCompression{"
      + "threshold=" + threshold
      + '}';
  }
}
