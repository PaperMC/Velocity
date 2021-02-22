package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

public interface Packet {

  @Deprecated
  default void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  default void encode(ByteBuf buf, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  boolean handle(PacketHandler handler);

  // TODO: Move this into decoder
  default int expectedMinLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return 0;
  }

  default int expectedMaxLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return -1;
  }
}
