/*
 * Copyright (C) 2018 Velocity Contributors
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
import com.velocitypowered.api.proxy.connection.InboundConnection;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.translation.GlobalTranslator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class InitialInboundConnection implements InboundConnection,
    MinecraftConnectionAssociation {

  private static final Logger logger = LogManager.getLogger(InitialInboundConnection.class);

  private final MinecraftConnection connection;
  private final String cleanedAddress;
  private final ServerboundHandshakePacket handshake;

  InitialInboundConnection(MinecraftConnection connection, String cleanedAddress,
      ServerboundHandshakePacket handshake) {
    this.connection = connection;
    this.cleanedAddress = cleanedAddress;
    this.handshake = handshake;
  }

  @Override
  public InetSocketAddress remoteAddress() {
    return (InetSocketAddress) connection.getRemoteAddress();
  }

  @Override
  public Optional<InetSocketAddress> connectedHostname() {
    return Optional.of(InetSocketAddress.createUnresolved(cleanedAddress, handshake.getPort()));
  }

  @Override
  public boolean isActive() {
    return connection.getChannel().isActive();
  }

  @Override
  public ProtocolVersion protocolVersion() {
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
    Component translated = GlobalTranslator.render(reason, Locale.getDefault());

    logger.info("{} has disconnected: {}", this,
        LegacyComponentSerializer.legacySection().serialize(translated));
    connection.closeWith(ClientboundDisconnectPacket.create(translated, protocolVersion()));
  }

  /**
   * Disconnects the connection from the server silently.
   * @param reason the reason for disconnecting
   */
  public void disconnectQuietly(Component reason) {
    Component translated = GlobalTranslator.render(reason, Locale.getDefault());
    connection.closeWith(ClientboundDisconnectPacket.create(translated, protocolVersion()));
  }
}
