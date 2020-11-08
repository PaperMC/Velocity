package com.velocitypowered.proxy.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;
import java.util.function.Supplier;

public interface Packet {

  @Deprecated
  default void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion protocolVersion) {
    throw new UnsupportedOperationException();
  }

  void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion protocolVersion);

  boolean handle(MinecraftSessionHandler handler);

  interface Decoder<P extends Packet> {
    P decode(final ByteBuf buf, final ProtocolDirection direction, final ProtocolVersion version);

    static <P extends Packet> Decoder<P> unsupported() {
      return (buf, direction, version) -> {
        throw new UnsupportedOperationException();
      };
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
