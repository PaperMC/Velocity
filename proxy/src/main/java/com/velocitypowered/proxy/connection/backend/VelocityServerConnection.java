package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.VelocityServer.GSON;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN;
import static com.velocitypowered.proxy.network.Connections.HANDLER;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
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
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
  private long lastPingId;
  private long lastPingSent;
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
   * @return a {@link com.velocitypowered.api.proxy.ConnectionRequestBuilder.Result} representing
   *         whether or not the connect succeeded
   */
  public CompletableFuture<Impl> connect() {
    CompletableFuture<Impl> result = new CompletableFuture<>();
    // Note: we use the event loop for the connection the player is on. This reduces context
    // switches.
    server.createBootstrap(proxyPlayer.getConnection().eventLoop())
        .handler(server.getBackendChannelInitializer())
        .connect(registeredServer.getServerInfo().getAddress())
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

  private String createLegacyForwardingAddress(UnaryOperator<List<Property>> propertiesTransform) {
    // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
    // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
    // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
    StringBuilder data = new StringBuilder()
        .append(registeredServer.getServerInfo().getAddress().getHostString())
        .append('\0')
        .append(proxyPlayer.getRemoteAddress().getHostString())
        .append('\0')
        .append(proxyPlayer.getGameProfile().getUndashedId())
        .append('\0');
    GSON.toJson(propertiesTransform.apply(proxyPlayer.getGameProfile().getProperties()), data);
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
    PlayerInfoForwarding forwardingMode = server.getConfiguration().getPlayerInfoForwardingMode();

    // Initiate the handshake.
    ProtocolVersion protocolVersion = proxyPlayer.getConnection().getNextProtocolVersion();
    Handshake handshake = new Handshake();
    handshake.setNextStatus(StateRegistry.LOGIN_ID);
    handshake.setProtocolVersion(protocolVersion);
    if (forwardingMode == PlayerInfoForwarding.LEGACY) {
      handshake.setServerAddress(createLegacyForwardingAddress());
    } else if (forwardingMode == PlayerInfoForwarding.BUNGEEGUARD) {
      byte[] secret = server.getConfiguration().getForwardingSecret();
      handshake.setServerAddress(createBungeeGuardForwardingAddress(secret));
    } else if (proxyPlayer.getConnection().getType() == ConnectionTypes.LEGACY_FORGE) {
      handshake.setServerAddress(handshake.getServerAddress() + HANDSHAKE_HOSTNAME_TOKEN);
    } else {
      handshake.setServerAddress(registeredServer.getServerInfo().getAddress().getHostString());
    }
    handshake.setPort(registeredServer.getServerInfo().getAddress().getPort());
    mc.delayedWrite(handshake);

    mc.setProtocolVersion(protocolVersion);
    mc.setState(StateRegistry.LOGIN);
    mc.delayedWrite(new ServerLogin(proxyPlayer.getUsername()));
    mc.flush();
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
  public VelocityRegisteredServer getServer() {
    return registeredServer;
  }

  @Override
  public ServerInfo getServerInfo() {
    return registeredServer.getServerInfo();
  }

  @Override
  public ConnectedPlayer getPlayer() {
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
    return "[server connection] " + proxyPlayer.getGameProfile().getName() + " -> "
        + registeredServer.getServerInfo().getName();
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    return sendPluginMessage(identifier, Unpooled.wrappedBuffer(data));
  }

  /**
   * Sends a plugin message to the server through this connection.
   * @param identifier the channel ID to use
   * @param data the data
   * @return whether or not the message was sent
   */
  public boolean sendPluginMessage(ChannelIdentifier identifier, ByteBuf data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");

    MinecraftConnection mc = ensureConnected();

    PluginMessage message = new PluginMessage(identifier.getId(), data);
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

  public long getLastPingId() {
    return lastPingId;
  }

  public long getLastPingSent() {
    return lastPingSent;
  }

  void setLastPingId(long lastPingId) {
    this.lastPingId = lastPingId;
    this.lastPingSent = System.currentTimeMillis();
  }

  public void resetLastPingId() {
    this.lastPingId = -1;
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
   * Gets whether the {@link com.velocitypowered.proxy.protocol.packet.JoinGame}
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
