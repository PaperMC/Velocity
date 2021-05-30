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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEventImpl;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.network.java.packet.legacy.LegacyDisconnectPacket;
import com.velocitypowered.proxy.network.java.packet.legacy.LegacyHandshakePacket;
import com.velocitypowered.proxy.network.java.packet.legacy.LegacyPingPacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.java.states.ProtocolStates;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.registry.protocol.ProtocolRegistry;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

public class HandshakeSessionHandler implements MinecraftSessionHandler {

  private static final Logger LOGGER = LogManager.getLogger(HandshakeSessionHandler.class);

  private final MinecraftConnection connection;
  private final VelocityServer server;

  public HandshakeSessionHandler(MinecraftConnection connection, VelocityServer server) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.server = Preconditions.checkNotNull(server, "server");
  }

  @Override
  public boolean handle(LegacyPingPacket packet) {
    connection.setProtocolVersion(ProtocolVersion.LEGACY);
    StatusSessionHandler handler = new StatusSessionHandler(server, connection,
        new LegacyInboundConnection(connection, packet));
    connection.setSessionHandler(handler);
    handler.handle(packet);
    return true;
  }

  @Override
  public boolean handle(LegacyHandshakePacket packet) {
    connection.closeWith(LegacyDisconnectPacket
        .from(Component.text(
            "Your client is extremely old. Please update to a newer version of Minecraft.",
            NamedTextColor.RED)
        ));
    return true;
  }

  @Override
  public boolean handle(ServerboundHandshakePacket handshake) {
    InitialInboundConnection ic = new InitialInboundConnection(connection,
        cleanVhost(handshake.getServerAddress()), handshake);
    ProtocolRegistry nextState = getStateForProtocol(handshake.getNextStatus());
    if (nextState == null) {
      LOGGER.error("{} provided invalid protocol {}", ic, handshake.getNextStatus());
      connection.close(true);
    } else {
      connection.setProtocolVersion(handshake.getProtocolVersion());
      connection.setAssociation(ic);

      if (nextState == ProtocolStates.STATUS) {
        connection.setState(nextState);
        connection.setSessionHandler(new StatusSessionHandler(server, connection, ic));
      } else if (nextState == ProtocolStates.LOGIN) {
        this.handleLogin(handshake, ic);
      } else {
        // If you get this, it's a bug in Velocity.
        throw new AssertionError("getStateForProtocol provided invalid state!");
      }
    }

    return true;
  }

  private static @Nullable ProtocolRegistry getStateForProtocol(int status) {
    switch (status) {
      case ServerboundHandshakePacket.STATUS_ID:
        return ProtocolStates.STATUS;
      case ServerboundHandshakePacket.LOGIN_ID:
        return ProtocolStates.LOGIN;
      default:
        return null;
    }
  }

  private void handleLogin(ServerboundHandshakePacket handshake, InitialInboundConnection ic) {
    if (!ProtocolVersion.isSupported(handshake.getProtocolVersion())) {
      ic.disconnectQuietly(Component.translatable("multiplayer.disconnect.outdated_client"));
      return;
    }

    connection.setType(getHandshakeConnectionType(handshake));

    // If the proxy is configured for modern forwarding, we must deny connections from 1.12.2
    // and lower, otherwise IP information will never get forwarded.
    if (server.configuration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && handshake.getProtocolVersion().lt(ProtocolVersion.MINECRAFT_1_13)) {
      ic.disconnectQuietly(Component.translatable(
          "velocity.error.modern-forwarding-needs-new-client"));
      return;
    }

    connection.setAutoReading(false);
    connection.setState(ProtocolStates.LOGIN);
    server.eventManager().fire(new ConnectionHandshakeEventImpl(ic, handshake.getServerAddress()))
        .thenAcceptAsync(event -> {
          if (connection.isClosed()) {
            return;
          }

          @Nullable Component disconnectReason = event.result().reason();
          if (disconnectReason != null) {
            ic.disconnectQuietly(disconnectReason);
          } else {
            // if the handshake is changed, propagate the change
            if (!event.currentHostname().equals(event.originalHostname())) {
              ic.setCleanedHostname(cleanVhost(event.currentHostname()));
            }

            if (!Objects.equals(event.currentRemoteHostAddress(), ic.remoteAddress())) {
              ic.setRemoteAddress(event.currentRemoteHostAddress());
            }

            if (connection.getRemoteAddress() instanceof InetSocketAddress) {
              InetAddress address = ((InetSocketAddress) connection.getRemoteAddress())
                  .getAddress();
              if (!server.getIpAttemptLimiter().attempt(address)) {
                ic.disconnectQuietly(
                    Component.translatable("velocity.error.logging-in-too-fast"));
                return;
              }
            }

            connection.setSessionHandler(new LoginSessionHandler(server, connection, ic));
            connection.setAutoReading(true);
          }
        }, connection.eventLoop());
  }

  private ConnectionType getHandshakeConnectionType(ServerboundHandshakePacket handshake) {
    // Determine if we're using Forge (1.8 to 1.12, may not be the case in 1.13).
    if (handshake.getServerAddress().endsWith(LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN)
        && handshake.getProtocolVersion().lt(ProtocolVersion.MINECRAFT_1_13)) {
      return ConnectionTypes.LEGACY_FORGE;
    } else if (handshake.getProtocolVersion().lte(ProtocolVersion.MINECRAFT_1_7_6)) {
      // 1.7 Forge will not notify us during handshake. UNDETERMINED will listen for incoming
      // forge handshake attempts. Also sends a reset handshake packet on every transition.
      return ConnectionTypes.UNDETERMINED_17;
    } else {
      // Note for future implementation: Forge 1.13+ identifies itself using a slightly different
      // hostname token.
      return ConnectionTypes.VANILLA;
    }
  }

  /**
   * Cleans the specified virtual host hostname.
   *
   * @param hostname the host name to clean
   * @return the cleaned hostname
   */
  @VisibleForTesting
  static String cleanVhost(String hostname) {
    // Clean out any anything after any zero bytes (this includes BungeeCord forwarding and the
    // legacy Forge handshake indicator).
    String cleaned = hostname;
    int zeroIdx = cleaned.indexOf('\0');
    if (zeroIdx > -1) {
      cleaned = hostname.substring(0, zeroIdx);
    }

    // If we connect through an SRV record, there will be a period at the end (DNS usually elides
    // this ending octet).
    if (!cleaned.isEmpty() && cleaned.charAt(cleaned.length() - 1) == '.') {
      cleaned = cleaned.substring(0, cleaned.length() - 1);
    }
    return cleaned;
  }

  @Override
  public void handleGeneric(Packet packet) {
    // Unknown packet received. Better to close the connection.
    connection.close(true);
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    // Unknown packet received. Better to close the connection.
    connection.close(true);
  }

  private static class LegacyInboundConnection implements InboundConnection {

    private final MinecraftConnection connection;
    private final LegacyPingPacket ping;

    private LegacyInboundConnection(MinecraftConnection connection,
        LegacyPingPacket ping) {
      this.connection = connection;
      this.ping = ping;
    }

    @Override
    public @Nullable SocketAddress remoteAddress() {
      return connection.getRemoteAddress();
    }

    @Override
    public @Nullable InetSocketAddress connectedHostname() {
      return ping.getVhost();
    }

    @Override
    public boolean isActive() {
      return !connection.isClosed();
    }

    @Override
    public ProtocolVersion protocolVersion() {
      return ProtocolVersion.LEGACY;
    }
  }
}
