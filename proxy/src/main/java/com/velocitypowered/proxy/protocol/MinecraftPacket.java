package com.velocitypowered.proxy.protocol;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public interface MinecraftPacket {

  void decode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion);

  void encode(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion);

  boolean handle(MinecraftSessionHandler handler);
}
