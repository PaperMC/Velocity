package com.velocitypowered.proxy.protocol.packet.legacy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.Packet;
import com.velocitypowered.proxy.protocol.ProtocolDirection;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyPingPacket implements Packet {

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

  public LegacyMinecraftPingVersion getVersion() {
    return version;
  }

  public @Nullable InetSocketAddress getVhost() {
    return vhost;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolDirection direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
