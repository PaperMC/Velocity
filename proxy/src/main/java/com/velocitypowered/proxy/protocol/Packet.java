package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public interface Packet {

  void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion protocolVersion);

  void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion protocolVersion);

  boolean handle(MinecraftSessionHandler handler);
}
