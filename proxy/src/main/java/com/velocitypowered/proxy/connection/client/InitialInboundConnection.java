package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import java.net.InetSocketAddress;
import java.util.Optional;

class InitialInboundConnection implements InboundConnection {

  private final MinecraftConnection connection;
  private final String cleanedAddress;
  private final Handshake handshake;

  InitialInboundConnection(MinecraftConnection connection, String cleanedAddress,
      Handshake handshake) {
    this.connection = connection;
    this.cleanedAddress = cleanedAddress;
    this.handshake = handshake;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) connection.getRemoteAddress();
  }

  @Override
  public Optional<InetSocketAddress> getVirtualHost() {
    return Optional.of(InetSocketAddress.createUnresolved(cleanedAddress, handshake.getPort()));
  }

  @Override
  public boolean isActive() {
    return connection.getChannel().isActive();
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return connection.getProtocolVersion();
  }
}
