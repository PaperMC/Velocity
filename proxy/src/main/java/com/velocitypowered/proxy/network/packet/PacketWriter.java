package com.velocitypowered.proxy.network.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;

public interface PacketWriter<P extends Packet> {
  void write(final ByteBuf out, final P packet, final ProtocolVersion version);

  static <P extends Packet> PacketWriter<P> unsupported() {
    return (buf, packet, version) -> {
      throw new UnsupportedOperationException();
    };
  }

  static <P extends Packet> PacketWriter<P> noop() {
    return (buf, packet, version) -> { };
  }

  @Deprecated
  static <P extends Packet> PacketWriter<P> deprecatedEncode() {
    return (buf, packet, version) -> packet.encode(buf, version);
  }
}