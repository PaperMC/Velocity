package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public interface MinecraftPacket {

  void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  boolean handle(MinecraftSessionHandler handler);

  default int expectedMaxLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return -1;
  }

  default int expectedMinLength(ByteBuf buf, ProtocolUtils.Direction direction,
      ProtocolVersion version) {
    return 0;
  }
}
