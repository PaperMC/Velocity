package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;

public interface PacketReader<P extends Packet> {
  P read(final ByteBuf buf, final ProtocolVersion version);

  default int expectedMinLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return 0;
  }

  default int expectedMaxLength(ByteBuf buf, PacketDirection direction, ProtocolVersion version) {
    return -1;
  }

  static <P extends Packet> PacketReader<P> unsupported() {
    return (buf, version) -> {
      throw new UnsupportedOperationException();
    };
  }

  static <P extends Packet> PacketReader<P> instance(final P packet) {
    return (buf, version) -> packet;
  }

  @Deprecated
  static <P extends Packet> PacketReader<P> method(final Supplier<P> factory) {
    return (buf, version) -> {
      final P packet = factory.get();
      packet.decode(buf, null, version);
      return packet;
    };
  }
}
