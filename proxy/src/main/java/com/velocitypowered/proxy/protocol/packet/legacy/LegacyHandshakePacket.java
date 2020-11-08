package com.velocitypowered.proxy.protocol.packet.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import io.netty.buffer.ByteBuf;

public class LegacyHandshakePacket implements LegacyPacket, Packet {

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
