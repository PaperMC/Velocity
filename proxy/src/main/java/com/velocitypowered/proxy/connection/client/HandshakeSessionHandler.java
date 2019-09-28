package com.velocitypowered.proxy.connection.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionType;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.LegacyDisconnect;
import com.velocitypowered.proxy.protocol.packet.LegacyHandshake;
import com.velocitypowered.proxy.protocol.packet.LegacyPing;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    StatusSessionHandler handler = new StatusSessionHandler(server, connection,
        new LegacyInboundConnection(connection, packet));
    connection.setSessionHandler(handler);
    handler.handle(packet);
    return true;
  }

  @Override
  public boolean handle(LegacyHandshake packet) {
    connection.closeWith(LegacyDisconnect
        .from(TextComponent.of("Your client is old, please upgrade!", TextColor.RED)));
    return true;
  }

  @Override
  public boolean handle(Handshake handshake) {
    InitialInboundConnection ic = new InitialInboundConnection(connection,
        cleanVhost(handshake.getServerAddress()), handshake);
    switch (handshake.getNextStatus()) {
      case StateRegistry.STATUS_ID:
        connection.setState(StateRegistry.STATUS);
        connection.setProtocolVersion(handshake.getProtocolVersion());
        connection.setAssociation(ic);
        connection.setSessionHandler(new StatusSessionHandler(server, connection, ic));
        return true;
      case StateRegistry.LOGIN_ID:
        connection.setState(StateRegistry.LOGIN);
        connection.setProtocolVersion(handshake.getProtocolVersion());

        if (!ProtocolVersion.isSupported(handshake.getProtocolVersion())) {
          connection.closeWith(Disconnect
              .create(TranslatableComponent.of("multiplayer.disconnect.outdated_client")));
          return true;
        }

        InetAddress address = ((InetSocketAddress) connection.getRemoteAddress()).getAddress();
        if (!server.getIpAttemptLimiter().attempt(address)) {
          connection.closeWith(
              Disconnect.create(TextComponent.of("You are logging in too fast, try again later.")));
          return true;
        }

        ConnectionType type = checkForForge(handshake);
        connection.setType(type);

        // Make sure legacy forwarding is not in use on this connection.
        if (!type.checkServerAddressIsValid(handshake.getServerAddress())) {
          connection.closeWith(Disconnect
              .create(TextComponent.of("Running Velocity behind Velocity is unsupported.")));
          return true;
        }

        // If the proxy is configured for modern forwarding, we must deny connections from 1.12.2
        // and lower, otherwise IP information will never get forwarded.
        if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
            && handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
          connection.closeWith(Disconnect
              .create(TextComponent.of("This server is only compatible with 1.13 and above.")));
          return true;
        }

        connection.setAssociation(ic);
        server.getEventManager().fireAndForget(new ConnectionHandshakeEvent(ic));
        connection.setSessionHandler(new LoginSessionHandler(server, connection, ic));
        return true;
      default:
        LOGGER.error("{} provided invalid protocol {}", ic, handshake.getNextStatus());
        connection.close();
        return true;
    }
  }

  private ConnectionType checkForForge(Handshake handshake) {
    // Determine if we're using Forge (1.8 to 1.12, may not be the case in 1.13).
    if (handshake.getServerAddress().endsWith(LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN)
        && handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      return ConnectionTypes.LEGACY_FORGE;
    } else if (handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_7_6) <= 0) {
      // 1.7 Forge will not notify us during handshake. UNDETERMINED will listen for incoming
      // forge handshake attempts. Also sends a reset handshake packet on every transition.
      return ConnectionTypes.UNDETERMINED_17;
    } else {
      // For later: See if we can determine Forge 1.13+ here, else this will need to be UNDETERMINED
      // until later in the cycle (most likely determinable during the LOGIN phase)
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
    connection.close();
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    // Unknown packet received. Better to close the connection.
    connection.close();
  }

  private static class LegacyInboundConnection implements InboundConnection {

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
  }
}
