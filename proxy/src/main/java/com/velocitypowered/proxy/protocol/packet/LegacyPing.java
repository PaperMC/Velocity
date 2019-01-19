package com.velocitypowered.proxy.protocol.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.packet.legacyping.LegacyMinecraftPingVersion;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

public class LegacyPing implements MinecraftPacket {

  private final LegacyMinecraftPingVersion version;
  @Nullable
  private final InetSocketAddress vhost;

  public LegacyPing(LegacyMinecraftPingVersion version) {
    this.version = version;
    this.vhost = null;
  }

  public LegacyPing(LegacyMinecraftPingVersion version, InetSocketAddress vhost) {
    this.version = version;
    this.vhost = vhost;
  }

  public LegacyMinecraftPingVersion getVersion() {
    return version;
  }

  @Nullable
  public InetSocketAddress getVhost() {
    return vhost;
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction, ProtocolVersion version) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }
}
