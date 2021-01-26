package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;

public class StatusRequest implements MinecraftPacket {

  public static final StatusRequest INSTANCE = new StatusRequest();

  private StatusRequest() {

  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    // There is no additional data to decode.
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
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

  @Override
  public int expectedMaxLength(ByteBuf buf, Direction direction, ProtocolVersion version) {
    return 0;
  }
}
