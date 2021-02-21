package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;

public interface Packet {

  @Deprecated
  default void decode(ByteBuf buf, PacketDirection direction, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  void encode(ByteBuf buf, PacketDirection direction, ProtocolVersion protocolVersion);

  boolean handle(PacketHandler handler);

  // TODO: Move this into decoder
  default int expectedMinLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return 0;
  }

  default int expectedMaxLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return -1;
  }
}
