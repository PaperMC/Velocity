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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import com.velocitypowered.proxy.protocol.packet.LegacyHandshake;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The initial handler used when a connection is established to the proxy. This will either
 * transition to {@link StatusSessionHandler} or {@link InitialLoginSessionHandler} as soon as the
 * handshake packet is received.
 */
public class HandshakeSessionHandler implements MinecraftSessionHandler {

  private static final Logger LOGGER = LogManager.getLogger(HandshakeSessionHandler.class);

  private final MinecraftConnection connection;
  private final VelocityServer server;

  public HandshakeSessionHandler(MinecraftConnection connection, VelocityServer server) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.server = Preconditions.checkNotNull(server, "server");
  }

  @Override
  public boolean handle(LegacyPing packet) {
    connection.setProtocolVersion(ProtocolVersion.LEGACY);
    StatusSessionHandler handler =
        new StatusSessionHandler(server, new LegacyInboundConnection(connection, packet));
    connection.setActiveSessionHandler(StateRegistry.STATUS, handler);
    handler.handle(packet);
    return true;
  }

  @Override
  public boolean handle(LegacyHandshake packet) {
    connection.closeWith(LegacyDisconnect.from(Component.text(
        "Your client is extremely old. Please update to a newer version of Minecraft.",
        NamedTextColor.RED)
    ));
    return true;
  }

  @Override
  public boolean handle(Handshake handshake) {
    InitialInboundConnection ic = new InitialInboundConnection(connection,
        cleanVhost(handshake.getServerAddress()), handshake);
    StateRegistry nextState = getStateForProtocol(handshake.getNextStatus());
    if (nextState == null) {
      LOGGER.error("{} provided invalid protocol {}", ic, handshake.getNextStatus());
      connection.close(true);
    } else {
      connection.setProtocolVersion(handshake.getProtocolVersion());
      connection.setAssociation(ic);

      switch (nextState) {
        case STATUS:
          connection.setActiveSessionHandler(StateRegistry.STATUS,
              new StatusSessionHandler(server, ic));
          break;
        case LOGIN:
          this.handleLogin(handshake, ic);
          break;
        default:
          // If you get this, it's a bug in Velocity.
          throw new AssertionError("getStateForProtocol provided invalid state!");
      }
    }

    return true;
  }

  private static @Nullable StateRegistry getStateForProtocol(int status) {
    switch (status) {
      case StateRegistry.STATUS_ID:
        return StateRegistry.STATUS;
      case StateRegistry.LOGIN_ID:
        return StateRegistry.LOGIN;
      default:
        return null;
    }
  }

  private void handleLogin(Handshake handshake, InitialInboundConnection ic) {
    if (!ProtocolVersion.isSupported(handshake.getProtocolVersion())) {
      ic.disconnectQuietly(Component.translatable("multiplayer.disconnect.outdated_client")
          .args(Component.text(ProtocolVersion.SUPPORTED_VERSION_STRING)));
      return;
    }

    InetAddress address = ((InetSocketAddress) connection.getRemoteAddress()).getAddress();
    if (!server.getIpAttemptLimiter().attempt(address)) {
      ic.disconnectQuietly(Component.translatable("velocity.error.logging-in-too-fast"));
      return;
    }

    connection.setType(getHandshakeConnectionType(handshake));

    // If the proxy is configured for modern forwarding, we must deny connections from 1.12.2
    // and lower, otherwise IP information will never get forwarded.
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      ic.disconnectQuietly(
          Component.translatable("velocity.error.modern-forwarding-needs-new-client"));
      return;
    }

    LoginInboundConnection lic = new LoginInboundConnection(ic);
    server.getEventManager().fireAndForget(new ConnectionHandshakeEvent(lic));
    connection.setActiveSessionHandler(StateRegistry.LOGIN,
        new InitialLoginSessionHandler(server, connection, lic));
  }

  private ConnectionType getHandshakeConnectionType(Handshake handshake) {
    // Determine if we're using Forge (1.8 to 1.12, may not be the case in 1.13).
    if (handshake.getServerAddress().endsWith(LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN)
        && handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      return ConnectionTypes.LEGACY_FORGE;
    } else if (handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_7_6) <= 0) {
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
  public void handleGeneric(MinecraftPacket packet) {
    // Unknown packet received. Better to close the connection.
    connection.close(true);
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    // Unknown packet received. Better to close the connection.
    connection.close(true);
  }

  private static class LegacyInboundConnection implements VelocityInboundConnection {

    private final MinecraftConnection connection;
    private final LegacyPing ping;

    private LegacyInboundConnection(MinecraftConnection connection,
        LegacyPing ping) {
      this.connection = connection;
      this.ping = ping;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return (InetSocketAddress) connection.getRemoteAddress();
    }

    @Override
    public Optional<InetSocketAddress> getVirtualHost() {
      return Optional.ofNullable(ping.getVhost());
    }

    @Override
    public boolean isActive() {
      return !connection.isClosed();
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
      return ProtocolVersion.LEGACY;
    }

    @Override
    public String toString() {
      return "[legacy connection] " + this.getRemoteAddress().toString();
    }

    @Override
    public MinecraftConnection getConnection() {
      return connection;
    }
  }
}
