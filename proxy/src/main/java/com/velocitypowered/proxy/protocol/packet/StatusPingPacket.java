package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import io.netty.buffer.ByteBuf;

public class StatusPingPacket implements Packet {

  public static Decoder<StatusPingPacket> DECODER = (buf, direction, version) -> {
    final long randomId = buf.readLong();
    return new StatusPingPacket(randomId);
  };

  private final long randomId;

  public StatusPingPacket(final long randomId) {
    this.randomId = randomId;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    buf.writeLong(randomId);
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
