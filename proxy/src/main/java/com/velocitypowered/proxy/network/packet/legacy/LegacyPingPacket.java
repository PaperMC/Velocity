package com.velocitypowered.proxy.network.packet.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketHandler;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyPingPacket implements LegacyPacket, Packet {

  private final LegacyMinecraftPingVersion version;
  private final @Nullable InetSocketAddress vhost;

  public LegacyPingPacket(LegacyMinecraftPingVersion version) {
    this.version = version;
    this.vhost = null;
  }

  public LegacyPingPacket(LegacyMinecraftPingVersion version, InetSocketAddress vhost) {
    this.version = version;
    this.vhost = vhost;
  }

  @Override
  public void encode(ByteBuf buf, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(PacketHandler handler) {
    return handler.handle(this);
  }

  public LegacyMinecraftPingVersion getVersion() {
    return version;
  }

  public @Nullable InetSocketAddress getVhost() {
    return vhost;
  }
}
