package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import io.netty.buffer.ByteBuf;

public class StatusRequestPacket implements Packet {

  public static final StatusRequestPacket INSTANCE = new StatusRequestPacket();

  private StatusRequestPacket() {

  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    // There is no additional data to decode.
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    // There is no data to decode.
  }

  @Override
  public String toString() {
    return "StatusRequest";
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
