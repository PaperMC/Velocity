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
    StateRegistry nextState = getStateForProtocol(handshake.getNextStatus());
    if (nextState == null) {
      LOGGER.error("{} provided invalid protocol {}", ic, handshake.getNextStatus());
      connection.close(true);
    } else {
      connection.setState(nextState);
      connection.setProtocolVersion(handshake.getProtocolVersion());
      connection.setAssociation(ic);

      switch (nextState) {
        case STATUS:
          connection.setSessionHandler(new StatusSessionHandler(server, connection, ic));
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
      connection.closeWith(Disconnect
          .create(TranslatableComponent.of("multiplayer.disconnect.outdated_client")));
      return;
    }

    InetAddress address = ((InetSocketAddress) connection.getRemoteAddress()).getAddress();
    if (!server.getIpAttemptLimiter().attempt(address)) {
      connection.closeWith(
          Disconnect.create(TextComponent.of("You are logging in too fast, try again later.")));
      return;
    }

    connection.setType(getHandshakeConnectionType(handshake));

    // If the proxy is configured for modern forwarding, we must deny connections from 1.12.2
    // and lower, otherwise IP information will never get forwarded.
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && handshake.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
      connection.closeWith(Disconnect
          .create(TextComponent.of("This server is only compatible with 1.13 and above.")));
      return;
    }

    server.getEventManager().fireAndForget(new ConnectionHandshakeEvent(ic));
    connection.setSessionHandler(new LoginSessionHandler(server, connection, ic));
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
