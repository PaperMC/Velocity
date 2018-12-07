package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.event.connection.ConnectionHandshakeEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
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
import com.velocitypowered.proxy.protocol.packet.LegacyPingResponse;
import io.netty.buffer.ByteBuf;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Optional;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;

public class HandshakeSessionHandler implements MinecraftSessionHandler {

  private final MinecraftConnection connection;
  private final VelocityServer server;

  public HandshakeSessionHandler(MinecraftConnection connection, VelocityServer server) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.server = Preconditions.checkNotNull(server, "server");
  }

  @Override
  public boolean handle(LegacyPing packet) {
    connection.setProtocolVersion(ProtocolVersion.LEGACY);
    VelocityConfiguration configuration = server.getConfiguration();
    ServerPing ping = new ServerPing(
        new ServerPing.Version(ProtocolVersion.MAXIMUM_VERSION.getProtocol(),
            "Velocity " + ProtocolVersion.SUPPORTED_VERSION_STRING),
        new ServerPing.Players(server.getPlayerCount(), configuration.getShowMaxPlayers(),
            ImmutableList.of()),
        configuration.getMotdComponent(),
        null,
        null
    );
    ProxyPingEvent event = new ProxyPingEvent(new LegacyInboundConnection(connection), ping);
    server.getEventManager().fire(event)
        .thenRunAsync(() -> {
          // The disconnect packet is the same as the server response one.
          LegacyPingResponse response = LegacyPingResponse.from(event.getPing());
          connection.closeWith(LegacyDisconnect.fromPingResponse(response));
        }, connection.eventLoop());
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

        server.getEventManager().fireAndForget(new ConnectionHandshakeEvent(ic));
        connection.setSessionHandler(new LoginSessionHandler(server, connection, ic));
        return true;
      default:
        throw new IllegalArgumentException("Invalid state " + handshake.getNextStatus());
    }
  }

  private ConnectionType checkForForge(Handshake handshake) {
    // Determine if we're using Forge (1.8 to 1.12, may not be the case in 1.13).
    if (handshake.getServerAddress().endsWith(LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN)
        && handshake.getProtocolVersion().getProtocol() < ProtocolVersion.MINECRAFT_1_13.getProtocol()) {
      return ConnectionTypes.LEGACY_FORGE;
    } else {
      // For later: See if we can determine Forge 1.13+ here, else this will need to be UNDETERMINED
      // until later in the cycle (most likely determinable during the LOGIN phase)
      return ConnectionTypes.VANILLA;
    }
  }

  private String cleanVhost(String hostname) {
    int zeroIdx = hostname.indexOf('\0');
    return zeroIdx == -1 ? hostname : hostname.substring(0, zeroIdx);
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

    private LegacyInboundConnection(MinecraftConnection connection) {
      this.connection = connection;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
      return (InetSocketAddress) connection.getRemoteAddress();
    }

    @Override
    public Optional<InetSocketAddress> getVirtualHost() {
      return Optional.empty();
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
