package com.velocitypowered.proxy.network.packet.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;

public class LegacyHandshakePacket implements LegacyPacket, Packet {

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }
}
