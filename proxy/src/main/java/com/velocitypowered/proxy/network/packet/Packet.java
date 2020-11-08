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

  interface Decoder<P extends Packet> {
    P decode(final ByteBuf buf, final PacketDirection direction, final ProtocolVersion version);

    static <P extends Packet> Decoder<P> unsupported() {
      return (buf, direction, version) -> {
        throw new UnsupportedOperationException();
      };
    }

    static <P extends Packet> Decoder<P> instance(final P packet) {
      return (buf, direction, version) -> packet;
    }

    @Deprecated
    static <P extends Packet> Decoder<P> method(final Supplier<P> factory) {
      return (buf, direction, version) -> {
        final P packet = factory.get();
        packet.decode(buf, direction, version);
        return packet;
      };
    }
  }
}
