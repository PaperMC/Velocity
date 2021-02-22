package com.velocitypowered.proxy.network.registry.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import io.netty.buffer.ByteBuf;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface PacketRegistryMap {
  @Nullable Packet readPacket(final int id, ByteBuf buf, ProtocolVersion version);

  <P extends Packet> void writePacket(P packet, ByteBuf buf, ProtocolVersion version);
}
