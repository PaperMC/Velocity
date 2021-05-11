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

package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN;
import static com.velocitypowered.proxy.network.HandlerNames.HANDLER;
import static com.velocitypowered.proxy.network.PluginMessageUtil.channelIdForVersion;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.connection.ServerConnection;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.api.proxy.player.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.api.util.GameProfile.Property;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.registry.DimensionRegistry;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.network.StateRegistry;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundJoinGamePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundServerLoginPacket;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityServerConnection implements MinecraftConnectionAssociation, ServerConnection {

  private final VelocityRegisteredServer registeredServer;
  private final ConnectedPlayer proxyPlayer;
  private final VelocityServer server;
  private @Nullable MinecraftConnection connection;
  private boolean hasCompletedJoin = false;
  private boolean gracefulDisconnect = false;
  private BackendConnectionPhase connectionPhase = BackendConnectionPhases.UNKNOWN;
  private final Map<Long, Long> pendingPings = new HashMap<>();
  private @MonotonicNonNull DimensionRegistry activeDimensionRegistry;

  /**
   * Initializes a new server connection.
   * @param registeredServer the server to connect to
   * @param proxyPlayer the player connecting to the server
   * @param server the Velocity proxy instance
   */
  public VelocityServerConnection(VelocityRegisteredServer registeredServer,
      ConnectedPlayer proxyPlayer, VelocityServer server) {
    this.registeredServer = registeredServer;
    this.proxyPlayer = proxyPlayer;
    this.server = server;
  }

  /**
   * Connects to the server.
   * @return a {@link ConnectionRequestBuilder.Result} representing
   *         whether or not the connect succeeded
   */
  public CompletableFuture<Impl> connect() {
    CompletableFuture<Impl> result = new CompletableFuture<>();
    // Note: we use the event loop for the connection the player is on. This reduces context
    // switches.
    SocketAddress destinationAddress = registeredServer.serverInfo().address();
    server.createBootstrap(proxyPlayer.getConnection().eventLoop(), destinationAddress)
        .handler(server.getBackendChannelInitializer())
        .connect(destinationAddress)
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            connection = new MinecraftConnection(future.channel(), server);
            connection.setAssociation(VelocityServerConnection.this);
            future.channel().pipeline().addLast(HANDLER, connection);

            // Kick off the connection process
            connection.setSessionHandler(
                new LoginSessionHandler(server, VelocityServerConnection.this, result));

            // Set the connection phase, which may, for future forge (or whatever), be determined
            // at this point already
            connectionPhase = connection.getType().getInitialBackendPhase();
            startHandshake();
          } else {
            // Complete the result immediately. ConnectedPlayer will reset the in-flight connection.
            result.completeExceptionally(future.cause());
          }
        });
    return result;
  }

  private String playerConnectedHostname() {
    return proxyPlayer.connectedHostname().map(InetSocketAddress::getHostString).orElse("");
  }

  private String createLegacyForwardingAddress(UnaryOperator<List<Property>> propertiesTransform) {
    // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
    // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
    // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
    SocketAddress playerRemoteAddress = proxyPlayer.remoteAddress();
    if (!(playerRemoteAddress instanceof InetSocketAddress)) {
      return playerConnectedHostname();
    }
    StringBuilder data = new StringBuilder()
        .append(playerConnectedHostname())
        .append('\0')
        .append(((InetSocketAddress) proxyPlayer.remoteAddress()).getHostString())
        .append('\0')
        .append(proxyPlayer.gameProfile().getUndashedId())
        .append('\0');
    GENERAL_GSON
        .toJson(propertiesTransform.apply(proxyPlayer.gameProfile().getProperties()), data);
    return data.toString();
  }

  private String createLegacyForwardingAddress() {
    return createLegacyForwardingAddress(UnaryOperator.identity());
  }

  private String createBungeeGuardForwardingAddress(byte[] forwardingSecret) {
    // Append forwarding secret as a BungeeGuard token.
    Property property = new Property("bungeeguard-token",
        new String(forwardingSecret, StandardCharsets.UTF_8), "");
    return createLegacyForwardingAddress(properties -> ImmutableList.<Property>builder()
        .addAll(properties)
        .add(property)
        .build());
  }

  private void startHandshake() {
    final MinecraftConnection mc = ensureConnected();
    PlayerInfoForwarding forwardingMode = server.configuration().getPlayerInfoForwardingMode();

    // Initiate the handshake.
    ProtocolVersion protocolVersion = proxyPlayer.getConnection().getProtocolVersion();
    String address = getHandshakeAddressField(forwardingMode);
    SocketAddress destinationAddr = registeredServer.serverInfo().address();
    int port = destinationAddr instanceof InetSocketAddress
        ? ((InetSocketAddress) destinationAddr).getPort() : 0;
    ServerboundHandshakePacket handshake = new ServerboundHandshakePacket(protocolVersion,
        address, port, StateRegistry.LOGIN_ID);
    mc.delayedWrite(handshake);

    mc.setProtocolVersion(protocolVersion);
    mc.setState(StateRegistry.LOGIN);
    mc.delayedWrite(new ServerboundServerLoginPacket(proxyPlayer.username()));
    mc.flush();
  }

  private String getHandshakeAddressField(PlayerInfoForwarding forwardingMode) {
    if (forwardingMode == PlayerInfoForwarding.LEGACY) {
      return createLegacyForwardingAddress();
    } else if (forwardingMode == PlayerInfoForwarding.BUNGEEGUARD) {
      byte[] secret = server.configuration().getForwardingSecret();
      return createBungeeGuardForwardingAddress(secret);
    } else if (proxyPlayer.getConnection().getType() == ConnectionTypes.LEGACY_FORGE) {
      return playerConnectedHostname() + HANDSHAKE_HOSTNAME_TOKEN;
    } else {
      return playerConnectedHostname();
    }
  }

  public @Nullable MinecraftConnection getConnection() {
    return connection;
  }

  /**
   * Ensures the connection is still active and throws an exception if it is not.
   * @return the active connection
   * @throws IllegalStateException if the connection is inactive
   */
  public MinecraftConnection ensureConnected() {
    if (connection == null) {
      throw new IllegalStateException("Not connected to server!");
    }
    return connection;
  }

  @Override
  public VelocityRegisteredServer target() {
    return registeredServer;
  }

  @Override
  public ServerInfo serverInfo() {
    return registeredServer.serverInfo();
  }

  @Override
  public ConnectedPlayer player() {
    return proxyPlayer;
  }

  /**
   * Disconnects from the server.
   */
  public void disconnect() {
    if (connection != null) {
      gracefulDisconnect = true;
      connection.close(false);
      connection = null;
    }
  }

  @Override
  public String toString() {
    return "[server connection] " + proxyPlayer.gameProfile().getName() + " -> "
        + registeredServer.serverInfo().name();
  }

  @Override
  public boolean sendPluginMessage(PluginChannelId identifier, byte[] data) {
    return sendPluginMessage(identifier, Unpooled.wrappedBuffer(data));
  }

  /**
   * Sends a plugin message to the server through this connection.
   * @param identifier the channel ID to use
   * @param data the data
   * @return whether or not the message was sent
   */
  public boolean sendPluginMessage(PluginChannelId identifier, ByteBuf data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");

    MinecraftConnection mc = ensureConnected();

    ServerboundPluginMessagePacket message = new ServerboundPluginMessagePacket(
        channelIdForVersion(identifier, mc.getProtocolVersion()), data);
    mc.write(message);
    return true;
  }

  /**
   * Indicates that we have completed the plugin process.
   */
  public void completeJoin() {
    if (!hasCompletedJoin) {
      hasCompletedJoin = true;
      if (connectionPhase == BackendConnectionPhases.UNKNOWN) {
        // Now we know
        connectionPhase = BackendConnectionPhases.VANILLA;
        if (connection != null) {
          connection.setType(ConnectionTypes.VANILLA);
        }
      }
    }
  }

  boolean isGracefulDisconnect() {
    return gracefulDisconnect;
  }

  public Map<Long, Long> getPendingPings() {
    return pendingPings;
  }

  /**
   * Ensures that this server connection remains "active": the connection is established and not
   * closed, the player is still connected to the server, and the player still remains online.
   *
   * @return whether or not the player is online
   */
  public boolean isActive() {
    return connection != null && !connection.isClosed() && !gracefulDisconnect
        && proxyPlayer.isActive();
  }

  /**
   * Gets the current "phase" of the connection, mostly used for tracking
   * modded negotiation for legacy forge servers and provides methods
   * for performing phase specific actions.
   *
   * @return The {@link BackendConnectionPhase}
   */
  public BackendConnectionPhase getPhase() {
    return connectionPhase;
  }

  /**
   * Sets the current "phase" of the connection. See {@link #getPhase()}
   *
   * @param connectionPhase The {@link BackendConnectionPhase}
   */
  public void setConnectionPhase(BackendConnectionPhase connectionPhase) {
    this.connectionPhase = connectionPhase;
  }

  /**
   * Gets whether the {@link ClientboundJoinGamePacket}
   * packet has been sent by this server.
   *
   * @return Whether the join has been completed.
   */
  public boolean hasCompletedJoin() {
    return hasCompletedJoin;
  }

  public DimensionRegistry getActiveDimensionRegistry() {
    return activeDimensionRegistry;
  }

  public void setActiveDimensionRegistry(DimensionRegistry activeDimensionRegistry) {
    this.activeDimensionRegistry = activeDimensionRegistry;
  }
}
