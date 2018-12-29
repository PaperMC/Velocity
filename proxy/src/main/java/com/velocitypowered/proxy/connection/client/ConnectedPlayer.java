package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.Notify;
import com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer;
import com.velocitypowered.api.event.player.PlayerModInfoEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.MessagePosition;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.api.util.title.TextTitle;
import com.velocitypowered.api.util.title.Title;
import com.velocitypowered.api.util.title.Titles;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import io.netty.buffer.ByteBufUtil;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.ComponentSerializers;
import net.kyori.text.serializer.PlainComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ConnectedPlayer implements MinecraftConnectionAssociation, Player {

  private static final PlainComponentSerializer PASS_THRU_TRANSLATE = new PlainComponentSerializer(
      c -> "", TranslatableComponent::key);
  static final PermissionProvider DEFAULT_PERMISSIONS = s -> PermissionFunction.ALWAYS_UNDEFINED;

  private static final Logger logger = LogManager.getLogger(ConnectedPlayer.class);

  private final MinecraftConnection connection;
  private final @Nullable InetSocketAddress virtualHost;
  private GameProfile profile;
  private PermissionFunction permissionFunction;
  private int tryIndex = 0;
  private long ping = -1;
  private @Nullable VelocityServerConnection connectedServer;
  private @Nullable VelocityServerConnection connectionInFlight;
  private @Nullable PlayerSettings settings;
  private @Nullable ModInfo modInfo;
  private final VelocityTabList tabList;
  private final VelocityServer server;
  private ClientConnectionPhase connectionPhase;

  @MonotonicNonNull
  private List<String> serversToTry = null;

  ConnectedPlayer(VelocityServer server, GameProfile profile, MinecraftConnection connection,
      @Nullable InetSocketAddress virtualHost) {
    this.server = server;
    this.tabList = new VelocityTabList(connection);
    this.profile = profile;
    this.connection = connection;
    this.virtualHost = virtualHost;
    this.permissionFunction = PermissionFunction.ALWAYS_UNDEFINED;
    this.connectionPhase = connection.getType().getInitialClientPhase();
  }

  @Override
  public String getUsername() {
    return profile.getName();
  }

  @Override
  public UUID getUniqueId() {
    return profile.getId();
  }

  @Override
  public Optional<ServerConnection> getCurrentServer() {
    return Optional.ofNullable(connectedServer);
  }

  public GameProfile getProfile() {
    return profile;
  }

  public MinecraftConnection getConnection() {
    return connection;
  }

  @Override
  public long getPing() {
    return this.ping;
  }

  void setPing(long ping) {
    this.ping = ping;
  }

  public PlayerSettings getPlayerSettings() {
    return settings == null ? ClientSettingsWrapper.DEFAULT : this.settings;
  }

  void setPlayerSettings(ClientSettings settings) {
    ClientSettingsWrapper cs = new ClientSettingsWrapper(settings);
    this.settings = cs;
    server.getEventManager().fireAndForget(new PlayerSettingsChangedEvent(this, cs));
  }

  public Optional<ModInfo> getModInfo() {
    return Optional.ofNullable(modInfo);
  }

  public void setModInfo(ModInfo modInfo) {
    this.modInfo = modInfo;
    server.getEventManager().fireAndForget(new PlayerModInfoEvent(this, modInfo));
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) connection.getRemoteAddress();
  }

  @Override
  public Optional<InetSocketAddress> getVirtualHost() {
    return Optional.ofNullable(virtualHost);
  }

  void setPermissionFunction(PermissionFunction permissionFunction) {
    this.permissionFunction = permissionFunction;
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
  public void sendMessage(Component component, MessagePosition position) {
    Preconditions.checkNotNull(component, "component");
    Preconditions.checkNotNull(position, "position");

    byte pos = (byte) position.ordinal();
    String json;
    if (position == MessagePosition.ACTION_BAR) {
      if (getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_11) >= 0) {
        // We can use the title packet instead.
        TitlePacket pkt = new TitlePacket();
        pkt.setAction(TitlePacket.SET_ACTION_BAR);
        pkt.setComponent(ComponentSerializers.JSON.serialize(component));
        connection.write(pkt);
        return;
      } else {
        // Due to issues with action bar packets, we'll need to convert the text message into a
        // legacy message and then inject the legacy text into a component... yuck!
        JsonObject object = new JsonObject();
        object.addProperty("text", ComponentSerializers.LEGACY.serialize(component));
        json = VelocityServer.GSON.toJson(object);
      }
    } else {
      json = ComponentSerializers.JSON.serialize(component);
    }

    Chat chat = new Chat();
    chat.setType(pos);
    chat.setMessage(json);
    connection.write(chat);
  }

  @Override
  public ConnectionRequestBuilder createConnectionRequest(RegisteredServer server) {
    return new ConnectionRequestBuilderImpl(server);
  }

  @Override
  public List<GameProfile.Property> getGameProfileProperties() {
    return this.profile.getProperties();
  }

  @Override
  public void setGameProfileProperties(List<GameProfile.Property> properties) {
    this.profile = profile.withProperties(Preconditions.checkNotNull(properties));
  }

  @Override
  public void setHeaderAndFooter(Component header, Component footer) {
    tabList.setHeaderAndFooter(header, footer);
  }

  @Override
  public void clearHeaderAndFooter() {
    tabList.clearHeaderAndFooter();
  }

  @Override
  public VelocityTabList getTabList() {
    return tabList;
  }

  @Override
  public void disconnect(Component reason) {
    logger.info("{} has disconnected: {}", this, ComponentSerializers.LEGACY.serialize(reason));
    connection.closeWith(Disconnect.create(reason));
  }

  @Override
  public void sendTitle(Title title) {
    Preconditions.checkNotNull(title, "title");

    if (title.equals(Titles.reset())) {
      connection.write(TitlePacket.resetForProtocolVersion(connection.getProtocolVersion()));
    } else if (title.equals(Titles.hide())) {
      connection.write(TitlePacket.hideForProtocolVersion(connection.getProtocolVersion()));
    } else if (title instanceof TextTitle) {
      TextTitle tt = (TextTitle) title;

      if (tt.isResetBeforeSend()) {
        connection
            .delayedWrite(TitlePacket.resetForProtocolVersion(connection.getProtocolVersion()));
      }

      Optional<Component> titleText = tt.getTitle();
      if (titleText.isPresent()) {
        TitlePacket titlePkt = new TitlePacket();
        titlePkt.setAction(TitlePacket.SET_TITLE);
        titlePkt.setComponent(ComponentSerializers.JSON.serialize(titleText.get()));
        connection.delayedWrite(titlePkt);
      }

      Optional<Component> subtitleText = tt.getSubtitle();
      if (subtitleText.isPresent()) {
        TitlePacket titlePkt = new TitlePacket();
        titlePkt.setAction(TitlePacket.SET_SUBTITLE);
        titlePkt.setComponent(ComponentSerializers.JSON.serialize(subtitleText.get()));
        connection.delayedWrite(titlePkt);
      }

      if (tt.areTimesSet()) {
        TitlePacket timesPkt = TitlePacket.timesForProtocolVersion(connection.getProtocolVersion());
        timesPkt.setFadeIn(tt.getFadeIn());
        timesPkt.setStay(tt.getStay());
        timesPkt.setFadeOut(tt.getFadeOut());
        connection.delayedWrite(timesPkt);
      }
      connection.flush();
    } else {
      throw new IllegalArgumentException("Unknown title class " + title.getClass().getName());
    }

  }

  @Nullable
  public VelocityServerConnection getConnectedServer() {
    return connectedServer;
  }

  public void resetInFlightConnection() {
    connectionInFlight = null;
  }

  public void handleConnectionException(RegisteredServer server, Throwable throwable) {
    if (throwable == null) {
      throw new NullPointerException("throwable");
    }

    Throwable wrapped = throwable;
    if (throwable instanceof CompletionException) {
      Throwable cause = throwable.getCause();
      if (cause != null) {
        wrapped = cause;
      }
    }
    String userMessage;
    if (connectedServer != null && connectedServer.getServerInfo().equals(server.getServerInfo())) {
      userMessage = "Your connection to " + server.getServerInfo().getName() + " encountered an "
          + " error.";
    } else {
      logger.error("{}: unable to connect to server {}", this, server.getServerInfo().getName(),
          wrapped);
      userMessage = "Unable to connect to " + server.getServerInfo().getName() + ". Try again "
          + "later.";
    }
    handleConnectionException(server, null, TextComponent.of(userMessage, TextColor.RED));
  }

  public void handleConnectionException(RegisteredServer server, Disconnect disconnect) {
    Component disconnectReason = ComponentSerializers.JSON.deserialize(disconnect.getReason());
    String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
    if (connectedServer != null && connectedServer.getServerInfo().equals(server.getServerInfo())) {
      logger.error("{}: kicked from server {}: {}", this, server.getServerInfo().getName(),
          plainTextReason);
      handleConnectionException(server, disconnectReason, TextComponent.builder()
          .content("Kicked from " + server.getServerInfo().getName() + ": ")
          .color(TextColor.RED)
          .append(disconnectReason)
          .build());
    } else {
      logger.error("{}: disconnected while connecting to {}: {}", this,
          server.getServerInfo().getName(), plainTextReason);
      handleConnectionException(server, disconnectReason, TextComponent.builder()
          .content("Can't connect to server " + server.getServerInfo().getName() + ": ")
          .color(TextColor.RED)
          .append(disconnectReason)
          .build());
    }
  }

  private void handleConnectionException(RegisteredServer rs, @Nullable Component kickReason,
      Component friendlyReason) {
    // There can't be any connection in flight now.
    connectionInFlight = null;

    if (connectedServer == null) {
      // The player isn't yet connected to a server.
      Optional<RegisteredServer> nextServer = getNextServerToTry();
      if (nextServer.isPresent()) {
        createConnectionRequest(nextServer.get()).fireAndForget();
      } else {
        disconnect(friendlyReason);
      }
    } else {
      KickedFromServerEvent originalEvent = new KickedFromServerEvent(this, rs, kickReason,
          !connectedServer.getServer().equals(rs), friendlyReason);
      server.getEventManager().fire(originalEvent)
          .thenAcceptAsync(event -> {
            if (event.getResult() instanceof DisconnectPlayer) {
              DisconnectPlayer res = (DisconnectPlayer) event.getResult();
              disconnect(res.getReason());
            } else if (event.getResult() instanceof RedirectPlayer) {
              RedirectPlayer res = (RedirectPlayer) event.getResult();
              createConnectionRequest(res.getServer()).fireAndForget();
            } else if (event.getResult() instanceof Notify) {
              Notify res = (Notify) event.getResult();
              if (event.kickedDuringServerConnect()) {
                sendMessage(res.getMessage());
              } else {
                disconnect(res.getMessage());
              }
            } else {
              // In case someone gets creative, assume we want to disconnect the player.
              disconnect(friendlyReason);
            }
          }, connection.eventLoop());
    }
  }

  public Optional<RegisteredServer> getNextServerToTry() {
    if (serversToTry == null) {
      String virtualHostStr = getVirtualHost().map(InetSocketAddress::getHostString).orElse("");
      serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(virtualHostStr,
          Collections.emptyList());
    }

    if (serversToTry.isEmpty()) {
      serversToTry = server.getConfiguration().getAttemptConnectionOrder();
    }

    if (tryIndex >= serversToTry.size()) {
      return Optional.empty();
    }

    String toTryName = serversToTry.get(tryIndex);
    tryIndex++;
    return server.getServer(toTryName);
  }

  public void setConnectedServer(@Nullable VelocityServerConnection serverConnection) {
    this.connectedServer = serverConnection;
    this.tryIndex = 0; // reset since we got connected to a server

    if (serverConnection == connectionInFlight) {
      connectionInFlight = null;
    }
  }

  public void sendLegacyForgeHandshakeResetPacket() {
    connectionPhase.resetConnectionPhase(this);
  }

  private MinecraftConnection ensureBackendConnection() {
    VelocityServerConnection sc = this.connectedServer;
    if (sc == null) {
      throw new IllegalStateException("No backend connection");
    }

    MinecraftConnection mc = sc.getConnection();
    if (mc == null) {
      throw new IllegalStateException("Backend connection is not connected to a server");
    }

    return mc;
  }

  void teardown() {
    if (connectionInFlight != null) {
      connectionInFlight.disconnect();
    }
    if (connectedServer != null) {
      connectedServer.disconnect();
    }
    server.unregisterConnection(this);
    server.getEventManager().fireAndForget(new DisconnectEvent(this));
  }

  @Override
  public String toString() {
    return "[connected player] " + profile.getName() + " (" + getRemoteAddress() + ")";
  }

  @Override
  public Tristate getPermissionValue(String permission) {
    return permissionFunction.getPermissionValue(permission);
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");
    PluginMessage message = new PluginMessage();
    message.setChannel(identifier.getId());
    message.setData(data);
    connection.write(message);
    return true;
  }

  @Override
  public void spoofChatInput(String input) {
    Preconditions.checkArgument(input.length() <= Chat.MAX_SERVERBOUND_MESSAGE_LENGTH,
        "input cannot be greater than " + Chat.MAX_SERVERBOUND_MESSAGE_LENGTH
            + " characters in length");
    ensureBackendConnection().write(Chat.createServerbound(input));
  }

  @Override
  public void sendResourcePack(String url) {
    Preconditions.checkNotNull(url, "url");

    ResourcePackRequest request = new ResourcePackRequest();
    request.setUrl(url);
    request.setHash("");
    connection.write(request);
  }

  @Override
  public void sendResourcePack(String url, byte[] hash) {
    Preconditions.checkNotNull(url, "url");
    Preconditions.checkNotNull(hash, "hash");
    Preconditions.checkArgument(hash.length == 20, "Hash length is not 20");

    ResourcePackRequest request = new ResourcePackRequest();
    request.setUrl(url);
    request.setHash(ByteBufUtil.hexDump(hash));
    connection.write(request);
  }

  /**
   * Sends a {@link KeepAlive} packet to the player with a random ID.
   * The response will be ignored by Velocity as it will not match the
   * ID last sent by the server.
   */
  public void sendKeepAlive() {
    if (connection.getState() == StateRegistry.PLAY) {
      KeepAlive keepAlive = new KeepAlive();
      keepAlive.setRandomId(ThreadLocalRandom.current().nextLong());
      connection.write(keepAlive);
    }
  }

  /**
   * Gets the current "phase" of the connection, mostly used for tracking
   * modded negotiation for legacy forge servers and provides methods
   * for performing phase specific actions.
   *
   * @return The {@link ClientConnectionPhase}
   */
  public ClientConnectionPhase getPhase() {
    return connectionPhase;
  }

  /**
   * Sets the current "phase" of the connection. See {@link #getPhase()}
   *
   * @param connectionPhase The {@link ClientConnectionPhase}
   */
  public void setPhase(ClientConnectionPhase connectionPhase) {
    this.connectionPhase = connectionPhase;
  }

  private class ConnectionRequestBuilderImpl implements ConnectionRequestBuilder {

    private final RegisteredServer toConnect;

    ConnectionRequestBuilderImpl(RegisteredServer toConnect) {
      this.toConnect = Preconditions.checkNotNull(toConnect, "info");
    }

    @Override
    public RegisteredServer getServer() {
      return toConnect;
    }

    private Optional<ConnectionRequestBuilder.Status> checkServer(RegisteredServer server) {
      Preconditions
          .checkState(server instanceof VelocityRegisteredServer, "Not a valid Velocity server.");
      if (connectionInFlight != null || (connectedServer != null
          && !connectedServer.hasCompletedJoin())) {
        return Optional.of(ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS);
      }
      if (connectedServer != null && connectedServer.getServer().equals(server)) {
        return Optional.of(ConnectionRequestBuilder.Status.ALREADY_CONNECTED);
      }
      return Optional.empty();
    }

    @Override
    public CompletableFuture<Result> connect() {
      Optional<ConnectionRequestBuilder.Status> initialCheck = checkServer(toConnect);
      if (initialCheck.isPresent()) {
        return CompletableFuture
            .completedFuture(ConnectionRequestResults.plainResult(initialCheck.get()));
      }

      // Otherwise, initiate the connection.
      ServerPreConnectEvent event = new ServerPreConnectEvent(ConnectedPlayer.this, toConnect);
      return server.getEventManager().fire(event)
          .thenCompose(newEvent -> {
            Optional<RegisteredServer> connectTo = newEvent.getResult().getServer();
            if (!connectTo.isPresent()) {
              return CompletableFuture.completedFuture(
                  ConnectionRequestResults
                      .plainResult(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED)
              );
            }

            RegisteredServer rs = connectTo.get();
            Optional<ConnectionRequestBuilder.Status> lastCheck = checkServer(rs);
            if (lastCheck.isPresent()) {
              return CompletableFuture
                  .completedFuture(ConnectionRequestResults.plainResult(lastCheck.get()));
            }

            VelocityRegisteredServer vrs = (VelocityRegisteredServer) rs;
            VelocityServerConnection con = new VelocityServerConnection(vrs, ConnectedPlayer.this,
                server);
            connectionInFlight = con;
            return con.connect();
          });
    }

    @Override
    public CompletableFuture<Boolean> connectWithIndication() {
      return connect()
          .whenCompleteAsync((status, throwable) -> {
            if (throwable != null) {
              handleConnectionException(toConnect, throwable);
              return;
            }

            switch (status.getStatus()) {
              case ALREADY_CONNECTED:
                sendMessage(ConnectionMessages.ALREADY_CONNECTED);
                break;
              case CONNECTION_IN_PROGRESS:
                sendMessage(ConnectionMessages.IN_PROGRESS);
                break;
              case CONNECTION_CANCELLED:
                // Ignored; the plugin probably already handled this.
                break;
              case SERVER_DISCONNECTED:
                handleConnectionException(toConnect, Disconnect.create(status.getReason()
                    .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR)));
                break;
              default:
                // The only remaining value is successful (no need to do anything!)
                break;
            }
          }, connection.eventLoop())
          .thenApply(Result::isSuccessful);
    }

    @Override
    public void fireAndForget() {
      connectWithIndication();
    }
  }
}
