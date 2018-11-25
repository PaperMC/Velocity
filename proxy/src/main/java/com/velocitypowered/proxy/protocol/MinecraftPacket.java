package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public interface MinecraftPacket {

  void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion protocolVersion);

  boolean handle(MinecraftSessionHandler handler);
}
