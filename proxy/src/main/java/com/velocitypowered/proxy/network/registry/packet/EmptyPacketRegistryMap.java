package com.velocitypowered.proxy.network.registry.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class EmptyPacketRegistryMap implements PacketRegistryMap {

  public static final EmptyPacketRegistryMap INSTANCE = new EmptyPacketRegistryMap();

  private EmptyPacketRegistryMap() {

  }

  @Override
  public @Nullable Packet readPacket(int id, ByteBuf buf, ProtocolVersion version) {
    return null;
  }

  @Override
  public <P extends Packet> void writePacket(P packet, ByteBuf buf, ProtocolVersion version) {
    throw new IllegalArgumentException(String.format(
        "Unable to find id for packet of type %s in protocol %s",
        packet.getClass().getName(), version
    ));
  }
}
