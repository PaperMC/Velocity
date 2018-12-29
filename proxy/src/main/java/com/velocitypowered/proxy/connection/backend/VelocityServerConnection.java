package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.VelocityServer.GSON;
import static com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants.HANDSHAKE_HOSTNAME_TOKEN;
import static com.velocitypowered.proxy.network.Connections.FRAME_DECODER;
import static com.velocitypowered.proxy.network.Connections.FRAME_ENCODER;
import static com.velocitypowered.proxy.network.Connections.HANDLER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_DECODER;
import static com.velocitypowered.proxy.network.Connections.MINECRAFT_ENCODER;
import static com.velocitypowered.proxy.network.Connections.READ_TIMEOUT;

import com.google.common.base.Preconditions;
import com.google.common.base.VerifyException;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintFrameDecoder;
import com.velocitypowered.proxy.protocol.netty.MinecraftVarintLengthEncoder;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
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
  public CompletableFuture<ConnectionRequestBuilder.Result> connect() {
    CompletableFuture<ConnectionRequestBuilder.Result> result = new CompletableFuture<>();
    server.initializeGenericBootstrap()
        .handler(new ChannelInitializer<Channel>() {
          @Override
          protected void initChannel(Channel ch) throws Exception {
            ch.pipeline()
                .addLast(READ_TIMEOUT,
                    new ReadTimeoutHandler(server.getConfiguration().getReadTimeout(),
                        TimeUnit.SECONDS))
                .addLast(FRAME_DECODER, new MinecraftVarintFrameDecoder())
                .addLast(FRAME_ENCODER, MinecraftVarintLengthEncoder.INSTANCE)
                .addLast(MINECRAFT_DECODER,
                    new MinecraftDecoder(ProtocolUtils.Direction.CLIENTBOUND))
                .addLast(MINECRAFT_ENCODER,
                    new MinecraftEncoder(ProtocolUtils.Direction.SERVERBOUND));
          }
        })
        .connect(registeredServer.getServerInfo().getAddress())
        .addListener((ChannelFutureListener) future -> {
          if (future.isSuccess()) {
            connection = new MinecraftConnection(future.channel(), server);
            connection.setState(StateRegistry.HANDSHAKE);
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
            // We need to remember to reset the in-flight connection to allow connect() to work
            // properly.
            proxyPlayer.resetInFlightConnection();
            result.completeExceptionally(future.cause());
          }
        });
    return result;
  }

  private String createLegacyForwardingAddress() {
    // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
    // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
    // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
    StringBuilder data = new StringBuilder(2048)
        .append(registeredServer.getServerInfo().getAddress().getHostString())
        .append('\0')
        .append(proxyPlayer.getRemoteAddress().getHostString())
        .append('\0')
        .append(proxyPlayer.getProfile().getUndashedId())
        .append('\0');
    GSON.toJson(proxyPlayer.getProfile().getProperties(), data);
    return data.toString();
  }

  private void startHandshake() {
    MinecraftConnection mc = connection;
    if (mc == null) {
      throw new IllegalStateException("No connection established!");
    }

    PlayerInfoForwarding forwardingMode = server.getConfiguration().getPlayerInfoForwardingMode();

    // Initiate the handshake.
    ProtocolVersion protocolVersion = proxyPlayer.getConnection().getNextProtocolVersion();
    Handshake handshake = new Handshake();
    handshake.setNextStatus(StateRegistry.LOGIN_ID);
    handshake.setProtocolVersion(protocolVersion);
    if (forwardingMode == PlayerInfoForwarding.LEGACY) {
      handshake.setServerAddress(createLegacyForwardingAddress());
    } else if (proxyPlayer.getConnection().getType() == ConnectionTypes.LEGACY_FORGE) {
      handshake.setServerAddress(handshake.getServerAddress() + HANDSHAKE_HOSTNAME_TOKEN);
    } else {
      handshake.setServerAddress(registeredServer.getServerInfo().getAddress().getHostString());
    }
    handshake.setPort(registeredServer.getServerInfo().getAddress().getPort());
    mc.write(handshake);

    mc.setProtocolVersion(protocolVersion);
    mc.setState(StateRegistry.LOGIN);
    mc.write(new ServerLogin(proxyPlayer.getUsername()));
  }

  @Nullable
  public MinecraftConnection getConnection() {
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
      connection.close();
      connection = null;
    }
  }

  @Override
  public String toString() {
    return "[server connection] " + proxyPlayer.getProfile().getName() + " -> " + registeredServer
        .getServerInfo().getName();
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");

    MinecraftConnection mc = connection;
    if (mc == null) {
      throw new IllegalStateException("Not connected to a server!");
    }

    PluginMessage message = new PluginMessage();
    message.setChannel(identifier.getId());
    message.setData(data);
    mc.write(message);
    return true;
  }

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
  boolean isActive() {
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

}
