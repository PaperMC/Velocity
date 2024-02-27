/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.util.ClosestLocaleMatcher;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.translation.GlobalTranslator;

/**
 * Implements {@link InboundConnection} for a newly-established connection.
 */
public final class InitialInboundConnection implements VelocityInboundConnection,
    MinecraftConnectionAssociation {

  private static final ComponentLogger logger = ComponentLogger
      .logger(InitialInboundConnection.class);

  private final MinecraftConnection connection;
  private final String cleanedAddress;
  private final HandshakePacket handshake;

  InitialInboundConnection(MinecraftConnection connection, String cleanedAddress,
                           HandshakePacket handshake) {
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
    final boolean isPlayerAddressLoggingEnabled = connection.server.getConfiguration()
        .isPlayerAddressLoggingEnabled();
    final String playerIp =
        isPlayerAddressLoggingEnabled
            ? connection.getRemoteAddress().toString() : "<ip address withheld>";
    return "[initial connection] " + playerIp;
  }

  @Override
  public MinecraftConnection getConnection() {
    return connection;
  }

  /**
   * Disconnects the connection from the server.
   *
   * @param reason the reason for disconnecting
   */
  public void disconnect(Component reason) {
    Component translated = GlobalTranslator.render(reason, ClosestLocaleMatcher.INSTANCE
        .lookupClosest(Locale.getDefault()));
    if (connection.server.getConfiguration().isLogPlayerConnections()) {
      logger.info(Component.text(this + " has disconnected: ").append(translated));
    }
    connection.closeWith(DisconnectPacket.create(translated, getProtocolVersion(), connection.getState()));
  }

  /**
   * Disconnects the connection from the server silently.
   *
   * @param reason the reason for disconnecting
   */
  public void disconnectQuietly(Component reason) {
    Component translated = GlobalTranslator.render(reason, ClosestLocaleMatcher.INSTANCE
        .lookupClosest(Locale.getDefault()));
    connection.closeWith(DisconnectPacket.create(translated, getProtocolVersion(), connection.getState()));
  }
}