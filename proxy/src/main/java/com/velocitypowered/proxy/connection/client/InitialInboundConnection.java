package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import java.net.InetSocketAddress;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class InitialInboundConnection implements InboundConnection,
    MinecraftConnectionAssociation {

  private static final Logger logger = LogManager.getLogger(InitialInboundConnection.class);

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

  @Override
  public String toString() {
    return "[initial connection] " + connection.getRemoteAddress().toString();
  }

  /**
   * Disconnects the connection from the server.
   * @param reason the reason for disconnecting
   */
  public void disconnect(Component reason) {
    logger.info("{} has disconnected: {}", this,
        LegacyComponentSerializer.legacySection().serialize(reason));
    connection.closeWith(Disconnect.create(reason, getProtocolVersion()));
  }

  /**
   * Disconnects the connection from the server silently.
   * @param reason the reason for disconnecting
   */
  public void disconnectQuietly(Component reason) {
    connection.closeWith(Disconnect.create(reason, getProtocolVersion()));
  }
}
