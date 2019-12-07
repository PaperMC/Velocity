package com.velocitypowered.proxy.connection.client;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.Notify;
import com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.ServerKickResult;
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
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import com.velocitypowered.proxy.text.translation.TranslationManager;
import com.velocitypowered.proxy.tablist.VelocityTabListLegacy;
import com.velocitypowered.proxy.util.VelocityMessages;
import com.velocitypowered.proxy.util.bossbar.VelocityBossBar;
import com.velocitypowered.proxy.util.collect.CappedSet;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.text.Component;
import net.kyori.text.TextComponent;
import net.kyori.text.TranslatableComponent;
import net.kyori.text.format.TextColor;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ConnectedPlayer implements MinecraftConnectionAssociation, Player {

  private static final int MAX_PLUGIN_CHANNELS = 1024;
  private static final PlainComponentSerializer PASS_THRU_TRANSLATE = new PlainComponentSerializer(
      c -> "", TranslatableComponent::key);
  static final PermissionProvider DEFAULT_PERMISSIONS = s -> PermissionFunction.ALWAYS_UNDEFINED;

  private static final Logger logger = LogManager.getLogger(ConnectedPlayer.class);

  /**
   * The actual Minecraft connection. This is actually a wrapper object around the Netty channel.
   */
  private final MinecraftConnection connection;
  private final @Nullable InetSocketAddress virtualHost;
  private GameProfile profile;
  private PermissionFunction permissionFunction;
  private int tryIndex = 0;
  private long ping = -1;
  private final boolean onlineMode;
  private @Nullable VelocityServerConnection connectedServer;
  private @Nullable VelocityServerConnection connectionInFlight;
  private @Nullable PlayerSettings settings;
  private @Nullable ModInfo modInfo;
  private final VelocityTabList tabList;
  private final VelocityServer server;
  private ClientConnectionPhase connectionPhase;
  private final Collection<String> knownChannels;
  private final CompletableFuture<Void> teardownFuture = new CompletableFuture<>();
  private final Set<VelocityBossBar> bossBars = Sets.newConcurrentHashSet();

  private @MonotonicNonNull List<String> serversToTry = null;

  ConnectedPlayer(VelocityServer server, GameProfile profile, MinecraftConnection connection,
      @Nullable InetSocketAddress virtualHost, boolean onlineMode) {
    this.server = server;
    if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.tabList = new VelocityTabList(connection);
    } else {
      this.tabList = new VelocityTabListLegacy(connection);
    }
    this.profile = profile;
    this.connection = connection;
    this.virtualHost = virtualHost;
    this.permissionFunction = PermissionFunction.ALWAYS_UNDEFINED;
    this.connectionPhase = connection.getType().getInitialClientPhase();
    this.knownChannels = CappedSet.create(MAX_PLUGIN_CHANNELS);
    this.onlineMode = onlineMode;
  }

  public Set<VelocityBossBar> getBossBars() {
    return bossBars;
  }

  /**
   * Cleanup references of boss bars to this player.
   */
  private void cleanupBossBarReferences() {
    for (VelocityBossBar bossBar : ImmutableSet.copyOf(this.bossBars)) {
      bossBar.removePlayer(this);
    }
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

  /**
   * Makes sure the player is connected to a server and returns the server they are connected to.
   * @return the server the player is connected to
   */
  public VelocityServerConnection ensureAndGetCurrentServer() {
    VelocityServerConnection con = this.connectedServer;
    if (con == null) {
      throw new IllegalStateException("Not connected to server!");
    }
    return con;
  }

  @Override
  public GameProfile getGameProfile() {
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

  @Override
  public boolean isOnlineMode() {
    return onlineMode;
  }

  @Override
  public PlayerSettings getPlayerSettings() {
    return settings == null ? ClientSettingsWrapper.DEFAULT : this.settings;
  }

  void setPlayerSettings(ClientSettings settings) {
    ClientSettingsWrapper cs = new ClientSettingsWrapper(settings);
    this.settings = cs;
    updateTranslations();
    server.getEventManager().fireAndForget(new PlayerSettingsChangedEvent(this, cs));
  }

  private void updateTranslations() {
    bossBars.forEach(bossBar -> bossBar.updateTranslationsFor(this));
    connection.flush();
  }

  @Override
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

    TranslationManager translationManager = server.getTranslationManager();
    component = translationManager.translateComponent(this, component);

    byte pos = (byte) position.ordinal();
    String json;
    if (position == MessagePosition.ACTION_BAR) {
      if (getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_11) >= 0) {
        // We can use the title packet instead.
        TitlePacket pkt = new TitlePacket();
        pkt.setAction(TitlePacket.SET_ACTION_BAR);
        pkt.setComponent(GsonComponentSerializer.INSTANCE.serialize(component));
        connection.write(pkt);
        return;
      } else {
        // Due to issues with action bar packets, we'll need to convert the text message into a
        // legacy message and then inject the legacy text into a component... yuck!
        JsonObject object = new JsonObject();
        object.addProperty("text", LegacyComponentSerializer.legacy().serialize(component));
        json = object.toString();
      }
    } else {
      json = GsonComponentSerializer.INSTANCE.serialize(component);
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
    logger.info("{} has disconnected: {}", this,
        LegacyComponentSerializer.legacy().serialize(reason));
    connection.closeWith(Disconnect.create(reason));
  }

  @Override
  public void sendTitle(Title title) {
    Preconditions.checkNotNull(title, "title");

    ProtocolVersion protocolVersion = connection.getProtocolVersion();
    if (title.equals(Titles.reset())) {
      connection.write(TitlePacket.resetForProtocolVersion(protocolVersion));
    } else if (title.equals(Titles.hide())) {
      connection.write(TitlePacket.hideForProtocolVersion(protocolVersion));
    } else if (title instanceof TextTitle) {
      TextTitle tt = (TextTitle) title;

      if (tt.isResetBeforeSend()) {
        connection.delayedWrite(TitlePacket.resetForProtocolVersion(protocolVersion));
      }

      Optional<Component> titleText = tt.getTitle();
      if (titleText.isPresent()) {
        TitlePacket titlePkt = new TitlePacket();
        titlePkt.setAction(TitlePacket.SET_TITLE);
        Component translated = server.getTranslationManager()
            .translateComponent(this, titleText.get());
        titlePkt.setComponent(GsonComponentSerializer.INSTANCE.serialize(translated));
        connection.delayedWrite(titlePkt);
      }

      Optional<Component> subtitleText = tt.getSubtitle();
      if (subtitleText.isPresent()) {
        TitlePacket titlePkt = new TitlePacket();
        titlePkt.setAction(TitlePacket.SET_SUBTITLE);
        Component translated = server.getTranslationManager()
            .translateComponent(this, subtitleText.get());
        titlePkt.setComponent(GsonComponentSerializer.INSTANCE.serialize(translated));
        connection.delayedWrite(titlePkt);
      }

      if (tt.areTimesSet()) {
        TitlePacket timesPkt = TitlePacket.timesForProtocolVersion(protocolVersion);
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

  public @Nullable VelocityServerConnection getConnectedServer() {
    return connectedServer;
  }

  public @Nullable VelocityServerConnection getConnectionInFlight() {
    return connectionInFlight;
  }

  public void resetInFlightConnection() {
    connectionInFlight = null;
  }

  /**
   * Handles unexpected disconnects.
   * @param server the server we disconnected from
   * @param throwable the exception
   * @param safe whether or not we can safely reconnect to a new server
   */
  public void handleConnectionException(RegisteredServer server, Throwable throwable,
      boolean safe) {
    if (!isActive()) {
      // If the connection is no longer active, it makes no sense to try and recover it.
      return;
    }

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
          + "error.";
    } else {
      logger.error("{}: unable to connect to server {}", this, server.getServerInfo().getName(),
          wrapped);
      userMessage = "Unable to connect to " + server.getServerInfo().getName() + ". Try again "
          + "later.";
    }
    handleConnectionException(server, null, TextComponent.of(userMessage, TextColor.RED), safe);
  }

  /**
   * Handles unexpected disconnects.
   * @param server the server we disconnected from
   * @param disconnect the disconnect packet
   * @param safe whether or not we can safely reconnect to a new server
   */
  public void handleConnectionException(RegisteredServer server, Disconnect disconnect,
      boolean safe) {
    if (!isActive()) {
      // If the connection is no longer active, it makes no sense to try and recover it.
      return;
    }

    Component disconnectReason = GsonComponentSerializer.INSTANCE.deserialize(
        disconnect.getReason());
    String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
    if (connectedServer != null && connectedServer.getServerInfo().equals(server.getServerInfo())) {
      logger.error("{}: kicked from server {}: {}", this, server.getServerInfo().getName(),
          plainTextReason);
      handleConnectionException(server, disconnectReason, TextComponent.builder()
          .content("Kicked from " + server.getServerInfo().getName() + ": ")
          .color(TextColor.RED)
          .append(disconnectReason)
          .build(), safe);
    } else {
      logger.error("{}: disconnected while connecting to {}: {}", this,
          server.getServerInfo().getName(), plainTextReason);
      handleConnectionException(server, disconnectReason, TextComponent.builder()
          .content("Can't connect to server " + server.getServerInfo().getName() + ": ")
          .color(TextColor.RED)
          .append(disconnectReason)
          .build(), safe);
    }
  }

  private void handleConnectionException(RegisteredServer rs,
      @Nullable Component kickReason, Component friendlyReason, boolean safe) {
    if (!isActive()) {
      // If the connection is no longer active, it makes no sense to try and recover it.
      return;
    }

    if (!safe) {
      // /!\ IT IS UNSAFE TO CONTINUE /!\
      //
      // This is usually triggered by a failed Forge handshake.
      disconnect(friendlyReason);
      return;
    }

    boolean kickedFromCurrent = connectedServer == null || connectedServer.getServer().equals(rs);
    ServerKickResult result;
    if (kickedFromCurrent) {
      Optional<RegisteredServer> next = getNextServerToTry(rs);
      result = next.<ServerKickResult>map(RedirectPlayer::create)
          .orElseGet(() -> DisconnectPlayer.create(friendlyReason));
    } else {
      // If we were kicked by going to another server, the connection should not be in flight
      if (connectionInFlight != null && connectionInFlight.getServer().equals(rs)) {
        resetInFlightConnection();
      }
      result = Notify.create(friendlyReason);
    }
    KickedFromServerEvent originalEvent = new KickedFromServerEvent(this, rs, kickReason,
        !kickedFromCurrent, result);
    handleKickEvent(originalEvent, friendlyReason);
  }

  private void handleKickEvent(KickedFromServerEvent originalEvent, Component friendlyReason) {
    server.getEventManager().fire(originalEvent)
        .thenAcceptAsync(event -> {
          // There can't be any connection in flight now.
          connectionInFlight = null;

          if (event.getResult() instanceof DisconnectPlayer) {
            DisconnectPlayer res = (DisconnectPlayer) event.getResult();
            disconnect(res.getReason());
          } else if (event.getResult() instanceof RedirectPlayer) {
            RedirectPlayer res = (RedirectPlayer) event.getResult();
            createConnectionRequest(res.getServer())
                .connectWithIndication()
                .whenCompleteAsync((newResult, exception) -> {
                  if (newResult == null || !newResult) {
                    disconnect(friendlyReason);
                  } else {
                    if (res.getMessage() == null) {
                      sendMessage(VelocityMessages.MOVED_TO_NEW_SERVER.append(friendlyReason));
                    } else {
                      sendMessage(res.getMessage());
                    }
                  }
                }, connection.eventLoop());
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

  /**
   * Finds another server to attempt to log into, if we were unexpectedly disconnected from the
   * server.
   *
   * @return the next server to try
   */
  public Optional<RegisteredServer> getNextServerToTry() {
    return this.getNextServerToTry(null);
  }

  /**
   * Finds another server to attempt to log into, if we were unexpectedly disconnected from the
   * server.
   *
   * @param current the "current" server that the player is on, useful as an override
   *
   * @return the next server to try
   */
  private Optional<RegisteredServer> getNextServerToTry(@Nullable RegisteredServer current) {
    if (serversToTry == null) {
      String virtualHostStr = getVirtualHost().map(InetSocketAddress::getHostString).orElse("");
      serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(virtualHostStr,
          Collections.emptyList());
    }

    if (serversToTry.isEmpty()) {
      serversToTry = server.getConfiguration().getAttemptConnectionOrder();
    }

    for (int i = tryIndex; i < serversToTry.size(); i++) {
      String toTryName = serversToTry.get(i);
      if ((connectedServer != null && hasSameName(connectedServer.getServer(), toTryName))
          || (connectionInFlight != null && hasSameName(connectionInFlight.getServer(), toTryName))
          || (current != null && hasSameName(current, toTryName))) {
        continue;
      }

      tryIndex = i;
      return server.getServer(toTryName);
    }
    return Optional.empty();
  }

  private static boolean hasSameName(RegisteredServer server, String name) {
    return server.getServerInfo().getName().equalsIgnoreCase(name);
  }

  /**
   * Sets the player's new connected server and clears the in-flight connection.
   *
   * @param serverConnection the new server connection
   */
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
    server.getEventManager().fire(new DisconnectEvent(this))
            .thenRun(this::finishTeardown);
  }

  private void finishTeardown() {
    cleanupBossBarReferences();
    this.teardownFuture.complete(null);
  }

  public CompletableFuture<Void> getTeardownFuture() {
    return teardownFuture;
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
    PluginMessage message = new PluginMessage(identifier.getId(), Unpooled.wrappedBuffer(data));
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

  /**
   * Return all the plugin message channels "known" to the client.
   * @return the channels
   */
  public Collection<String> getKnownChannels() {
    return knownChannels;
  }

  /**
   * Determines whether or not we can forward a plugin message onto the client.
   * @param version the Minecraft protocol version
   * @param message the plugin message to forward to the client
   * @return {@code true} if the message can be forwarded, {@code false} otherwise
   */
  public boolean canForwardPluginMessage(ProtocolVersion version, PluginMessage message) {
    boolean minecraftOrFmlMessage;

    // By default, all internal Minecraft and Forge channels are forwarded from the server.
    if (version.compareTo(ProtocolVersion.MINECRAFT_1_12_2) <= 0) {
      String channel = message.getChannel();
      minecraftOrFmlMessage = channel.startsWith("MC|")
          || channel.startsWith(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL)
          || PluginMessageUtil.isLegacyRegister(message)
          || PluginMessageUtil.isLegacyUnregister(message);
    } else {
      minecraftOrFmlMessage = message.getChannel().startsWith("minecraft:");
    }

    // Otherwise, we need to see if the player already knows this channel or it's known by the
    // proxy.
    return minecraftOrFmlMessage || knownChannels.contains(message.getChannel());
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

    private CompletableFuture<Impl> internalConnect() {
      Optional<ConnectionRequestBuilder.Status> initialCheck = checkServer(toConnect);
      if (initialCheck.isPresent()) {
        return CompletableFuture
            .completedFuture(ConnectionRequestResults.plainResult(initialCheck.get(), toConnect));
      }

      // Otherwise, initiate the connection.
      ServerPreConnectEvent event = new ServerPreConnectEvent(ConnectedPlayer.this, toConnect);
      return server.getEventManager().fire(event)
          .thenCompose(newEvent -> {
            Optional<RegisteredServer> connectTo = newEvent.getResult().getServer();
            if (!connectTo.isPresent()) {
              return CompletableFuture.completedFuture(
                  ConnectionRequestResults
                      .plainResult(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED, toConnect)
              );
            }

            RegisteredServer rs = connectTo.get();
            Optional<ConnectionRequestBuilder.Status> lastCheck = checkServer(rs);
            if (lastCheck.isPresent()) {
              return CompletableFuture
                  .completedFuture(ConnectionRequestResults.plainResult(lastCheck.get(), rs));
            }

            VelocityRegisteredServer vrs = (VelocityRegisteredServer) rs;
            VelocityServerConnection con = new VelocityServerConnection(vrs, ConnectedPlayer.this,
                server);
            connectionInFlight = con;
            return con.connect();
          });
    }

    @Override
    public CompletableFuture<Result> connect() {
      return this.internalConnect()
          .whenCompleteAsync((status, throwable) -> {
            if (status != null && !status.isSafe()) {
              // If it's not safe to continue the connection we need to shut it down.
              handleConnectionException(status.getAttemptedConnection(), throwable, true);
            } else if ((status != null && !status.isSuccessful())) {
              resetInFlightConnection();
            }
          }, connection.eventLoop())
          .thenApply(x -> x);
    }

    @Override
    public CompletableFuture<Boolean> connectWithIndication() {
      return internalConnect()
          .whenCompleteAsync((status, throwable) -> {
            if (throwable != null) {
              // TODO: The exception handling from this is not very good. Find a better way.
              handleConnectionException(status != null ? status.getAttemptedConnection()
                  : toConnect, throwable, true);
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
                    .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR)), status.isSafe());
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
