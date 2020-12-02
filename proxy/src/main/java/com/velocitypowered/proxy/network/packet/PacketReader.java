package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;

public interface PacketReader<P extends Packet> {
  P read(final ByteBuf buf, final PacketDirection direction, final ProtocolVersion version);

  static <P extends Packet> PacketReader<P> unsupported() {
    return (buf, direction, version) -> {
      throw new UnsupportedOperationException();
    };
  }

  static <P extends Packet> PacketReader<P> instance(final P packet) {
    return (buf, direction, version) -> packet;
  }

  @Deprecated
  static <P extends Packet> PacketReader<P> method(final Supplier<P> factory) {
    return (buf, direction, version) -> {
      final P packet = factory.get();
      packet.decode(buf, direction, version);
      return packet;
    };
  }
}
