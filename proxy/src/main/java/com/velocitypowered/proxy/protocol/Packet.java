package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public interface Packet {

  @Deprecated
  default void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion protocolVersion) {
    throw new IllegalStateException();
  }

  void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion protocolVersion);

  boolean handle(MinecraftSessionHandler handler);

  interface Decoder<P extends Packet> {
    P decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version);
  }
}
