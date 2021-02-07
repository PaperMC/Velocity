package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.api.proxy.ConnectionRequestBuilder.Status.ALREADY_CONNECTED;
import static com.velocitypowered.proxy.connection.util.ConnectionRequestResults.plainResult;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
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
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabListLegacy;
import com.velocitypowered.proxy.util.DurationUtils;
import com.velocitypowered.proxy.util.collect.CappedSet;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ConnectedPlayer implements MinecraftConnectionAssociation, Player {

  private static final int MAX_PLUGIN_CHANNELS = 1024;
  private static final PlainComponentSerializer PASS_THRU_TRANSLATE = new PlainComponentSerializer(
      c -> "", TranslatableComponent::key);
  static final PermissionProvider DEFAULT_PERMISSIONS = s -> PermissionFunction.ALWAYS_UNDEFINED;

  private static final Logger logger = LogManager.getLogger(ConnectedPlayer.class);

  private final Identity identity = new IdentityImpl();
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
  private Component playerListHeader = Component.empty();
  private Component playerListFooter = Component.empty();
  private final VelocityTabList tabList;
  private final VelocityServer server;
  private ClientConnectionPhase connectionPhase;
  private final Collection<String> knownChannels;
  private final CompletableFuture<Void> teardownFuture = new CompletableFuture<>();
  private @MonotonicNonNull List<String> serversToTry = null;

  ConnectedPlayer(VelocityServer server, GameProfile profile, MinecraftConnection connection,
      @Nullable InetSocketAddress virtualHost, boolean onlineMode) {
    this.server = server;
    this.profile = profile;
    this.connection = connection;
    this.virtualHost = virtualHost;
    this.permissionFunction = PermissionFunction.ALWAYS_UNDEFINED;
    this.connectionPhase = connection.getType().getInitialClientPhase();
    this.knownChannels = CappedSet.create(MAX_PLUGIN_CHANNELS);
    this.onlineMode = onlineMode;

    if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.tabList = new VelocityTabList(this);
    } else {
      this.tabList = new VelocityTabListLegacy(this);
    }
  }

  @Override
  public @NonNull Identity identity() {
    return this.identity;
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
    server.getEventManager().fireAndForget(new PlayerSettingsChangedEvent(this, cs));
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
  public void sendMessage(net.kyori.text.Component component, MessagePosition position) {
    Preconditions.checkNotNull(component, "component");
    Preconditions.checkNotNull(position, "position");

    byte pos = (byte) position.ordinal();
    String json;
    if (position == MessagePosition.ACTION_BAR) {
      if (getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_11) >= 0) {
        // We can use the title packet instead.
        TitlePacket pkt = new TitlePacket();
        pkt.setAction(TitlePacket.SET_ACTION_BAR);
        pkt.setComponent(net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE
            .serialize(component));
        connection.write(pkt);
        return;
      } else {
        // Due to issues with action bar packets, we'll need to convert the text message into a
        // legacy message and then inject the legacy text into a component... yuck!
        JsonObject object = new JsonObject();
        object.addProperty("text", net.kyori.text.serializer.legacy
            .LegacyComponentSerializer.legacy().serialize(component));
        json = object.toString();
      }
    } else {
      json = net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE.serialize(component);
    }

    Chat chat = new Chat();
    chat.setType(pos);
    chat.setMessage(json);
    connection.write(chat);
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message) {
    connection.write(Chat.createClientbound(identity, message, this.getProtocolVersion()));
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message,
      @NonNull MessageType type) {
    Preconditions.checkNotNull(message, "message");
    Preconditions.checkNotNull(type, "type");

    Chat packet = Chat.createClientbound(identity, message, this.getProtocolVersion());
    packet.setType(type == MessageType.CHAT ? Chat.CHAT_TYPE : Chat.SYSTEM_TYPE);
    connection.write(packet);
  }

  @Override
  public void sendActionBar(net.kyori.adventure.text.@NonNull Component message) {
    ProtocolVersion playerVersion = getProtocolVersion();
    if (playerVersion.compareTo(ProtocolVersion.MINECRAFT_1_11) >= 0) {
      // Use the title packet instead.
      TitlePacket pkt = new TitlePacket();
      pkt.setAction(TitlePacket.SET_ACTION_BAR);
      pkt.setComponent(ProtocolUtils.getJsonChatSerializer(playerVersion)
          .serialize(message));
      connection.write(pkt);
    } else {
      // Due to issues with action bar packets, we'll need to convert the text message into a
      // legacy message and then inject the legacy text into a component... yuck!
      JsonObject object = new JsonObject();
      object.addProperty("text", LegacyComponentSerializer.legacySection().serialize(message));
      Chat chat = new Chat();
      chat.setMessage(object.toString());
      chat.setType(Chat.GAME_INFO_TYPE);
      connection.write(chat);
    }
  }

  @Override
  public Component getPlayerListHeader() {
    return this.playerListHeader;
  }

  @Override
  public Component getPlayerListFooter() {
    return this.playerListFooter;
  }

  @Override
  public void sendPlayerListHeader(@NonNull final Component header) {
    this.sendPlayerListHeaderAndFooter(header, this.playerListFooter);
  }

  @Override
  public void sendPlayerListFooter(@NonNull final Component footer) {
    this.sendPlayerListHeaderAndFooter(this.playerListHeader, footer);
  }

  @Override
  public void sendPlayerListHeaderAndFooter(final Component header, final Component footer) {
    this.playerListHeader = Objects.requireNonNull(header, "header");
    this.playerListFooter = Objects.requireNonNull(footer, "footer");
    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.connection.write(HeaderAndFooter.create(header, footer, this.getProtocolVersion()));
    }
  }

  @Override
  public void showTitle(net.kyori.adventure.title.@NonNull Title title) {
    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(this
          .getProtocolVersion());

      TitlePacket titlePkt = new TitlePacket();
      titlePkt.setAction(TitlePacket.SET_TITLE);
      titlePkt.setComponent(serializer.serialize(title.title()));
      connection.delayedWrite(titlePkt);

      TitlePacket subtitlePkt = new TitlePacket();
      subtitlePkt.setAction(TitlePacket.SET_SUBTITLE);
      subtitlePkt.setComponent(serializer.serialize(title.subtitle()));
      connection.delayedWrite(subtitlePkt);

      TitlePacket timesPkt = TitlePacket.timesForProtocolVersion(this.getProtocolVersion());
      net.kyori.adventure.title.Title.Times times = title.times();
      if (times != null) {
        timesPkt.setFadeIn((int) DurationUtils.toTicks(times.fadeIn()));
        timesPkt.setStay((int) DurationUtils.toTicks(times.stay()));
        timesPkt.setFadeOut((int) DurationUtils.toTicks(times.fadeOut()));
      }
      connection.delayedWrite(timesPkt);

      connection.flush();
    }
  }

  @Override
  public void clearTitle() {
    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(TitlePacket.hideForProtocolVersion(this.getProtocolVersion()));
    }
  }

  @Override
  public void resetTitle() {
    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(TitlePacket.resetForProtocolVersion(this.getProtocolVersion()));
    }
  }

  @Override
  public void hideBossBar(@NonNull BossBar bar) {
    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      this.server.getBossBarManager().removeBossBar(this, bar);
    }
  }

  @Override
  public void showBossBar(@NonNull BossBar bar) {
    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      this.server.getBossBarManager().addBossBar(this, bar);
    }
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

  @Deprecated
  @Override
  public void setHeaderAndFooter(net.kyori.text.Component header, net.kyori.text.Component footer) {
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
    if (connection.eventLoop().inEventLoop()) {
      disconnect0(reason, false);
    } else {
      connection.eventLoop().execute(() -> disconnect0(reason, false));
    }
  }

  @Override
  public void disconnect(net.kyori.text.Component reason) {
    if (connection.eventLoop().inEventLoop()) {
      disconnect0(reason, false);
    } else {
      connection.eventLoop().execute(() -> disconnect0(reason, false));
    }
  }

  /**
   * Disconnects the player from the proxy.
   * @param reason the reason for disconnecting the player
   * @param duringLogin whether the disconnect happened during login
   */
  public void disconnect0(net.kyori.text.Component reason, boolean duringLogin) {
    logger.info("{} has disconnected: {}", this,
        net.kyori.text.serializer.legacy.LegacyComponentSerializer.legacy().serialize(reason));
    connection.closeWith(Disconnect.create(reason));
  }

  /**
   * Disconnects the player from the proxy.
   * @param reason the reason for disconnecting the player
   * @param duringLogin whether the disconnect happened during login
   */
  public void disconnect0(Component reason, boolean duringLogin) {
    logger.info("{} has disconnected: {}", this,
        LegacyComponentSerializer.legacySection().serialize(reason));
    connection.closeWith(Disconnect.create(reason, this.getProtocolVersion()));
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

      Optional<net.kyori.text.Component> titleText = tt.getTitle();
      if (titleText.isPresent()) {
        TitlePacket titlePkt = new TitlePacket();
        titlePkt.setAction(TitlePacket.SET_TITLE);
        titlePkt.setComponent(net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE
            .serialize(titleText.get()));
        connection.delayedWrite(titlePkt);
      }

      Optional<net.kyori.text.Component> subtitleText = tt.getSubtitle();
      if (subtitleText.isPresent()) {
        TitlePacket titlePkt = new TitlePacket();
        titlePkt.setAction(TitlePacket.SET_SUBTITLE);
        titlePkt.setComponent(net.kyori.text.serializer.gson.GsonComponentSerializer.INSTANCE
            .serialize(subtitleText.get()));
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
    handleConnectionException(server, null, Component.text(userMessage,
        NamedTextColor.RED), safe);
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

    VelocityConfiguration.Messages messages = this.server.getConfiguration().getMessages();
    Component disconnectReason = GsonComponentSerializer.gson().deserialize(disconnect.getReason());
    String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
    if (connectedServer != null && connectedServer.getServerInfo().equals(server.getServerInfo())) {
      logger.error("{}: kicked from server {}: {}", this, server.getServerInfo().getName(),
          plainTextReason);
      handleConnectionException(server, disconnectReason, Component.text()
          .append(messages.getKickPrefix(server.getServerInfo().getName())
              .colorIfAbsent(NamedTextColor.RED))
          .color(NamedTextColor.RED)
          .append(disconnectReason)
          .build(), safe);
    } else {
      logger.error("{}: disconnected while connecting to {}: {}", this,
          server.getServerInfo().getName(), plainTextReason);
      handleConnectionException(server, disconnectReason, Component.text()
          .append(messages.getDisconnectPrefix(server.getServerInfo().getName())
              .colorIfAbsent(NamedTextColor.RED))
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
      result = next.map(RedirectPlayer::create)
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
    handleKickEvent(originalEvent, friendlyReason, kickedFromCurrent);
  }

  private void handleKickEvent(KickedFromServerEvent originalEvent, Component friendlyReason,
      boolean kickedFromCurrent) {
    server.getEventManager().fire(originalEvent)
        .thenAcceptAsync(event -> {
          // There can't be any connection in flight now.
          connectionInFlight = null;

          // Make sure we clear the current connected server as the connection is invalid.
          boolean previouslyConnected = connectedServer != null;
          if (kickedFromCurrent) {
            connectedServer = null;
          }

          if (!isActive()) {
            // If the connection is no longer active, it makes no sense to try and recover it.
            return;
          }

          if (event.getResult() instanceof DisconnectPlayer) {
            DisconnectPlayer res = (DisconnectPlayer) event.getResult();
            disconnect(res.getReasonComponent());
          } else if (event.getResult() instanceof RedirectPlayer) {
            RedirectPlayer res = (RedirectPlayer) event.getResult();
            createConnectionRequest(res.getServer())
                .connect()
                .whenCompleteAsync((status, throwable) -> {
                  if (throwable != null) {
                    handleConnectionException(status != null ? status.getAttemptedConnection()
                        : res.getServer(), throwable, true);
                    return;
                  }

                  switch (status.getStatus()) {
                    // Impossible/nonsensical cases
                    case ALREADY_CONNECTED:
                    case CONNECTION_IN_PROGRESS:
                    // Fatal case
                    case CONNECTION_CANCELLED:
                      disconnect(status.getReasonComponent().orElse(res.getMessageComponent()));
                      break;
                    case SERVER_DISCONNECTED:
                      Component reason = status.getReasonComponent()
                          .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
                      handleConnectionException(res.getServer(), Disconnect.create(reason,
                          getProtocolVersion()), ((Impl) status).isSafe());
                      break;
                    case SUCCESS:
                      sendMessage(Identity.nil(), server.getConfiguration().getMessages()
                          .getMovedToNewServerPrefix().append(friendlyReason));
                      break;
                    default:
                      // The only remaining value is successful (no need to do anything!)
                      break;
                  }
                }, connection.eventLoop());
          } else if (event.getResult() instanceof Notify) {
            Notify res = (Notify) event.getResult();
            if (event.kickedDuringServerConnect() && previouslyConnected) {
              sendMessage(Identity.nil(), res.getMessageComponent());
            } else {
              disconnect(res.getMessageComponent());
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

    Optional<Player> connectedPlayer = server.getPlayer(this.getUniqueId());
    server.unregisterConnection(this);

    DisconnectEvent.LoginStatus status;
    if (connectedPlayer.isPresent()) {
      if (!connectedPlayer.get().getCurrentServer().isPresent()) {
        status = LoginStatus.PRE_SERVER_JOIN;
      } else {
        status = connectedPlayer.get() == this ? LoginStatus.SUCCESSFUL_LOGIN
            : LoginStatus.CONFLICTING_LOGIN;
      }
    } else {
      status = connection.isKnownDisconnect() ? LoginStatus.CANCELLED_BY_PROXY :
          LoginStatus.CANCELLED_BY_USER;
    }

    DisconnectEvent event = new DisconnectEvent(this, status);
    server.getEventManager().fire(event).whenComplete((val, ex) -> {
      if (ex == null) {
        this.teardownFuture.complete(null);
      } else {
        this.teardownFuture.completeExceptionally(ex);
      }
    });
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

    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      ResourcePackRequest request = new ResourcePackRequest();
      request.setUrl(url);
      request.setHash("");
      connection.write(request);
    }
  }

  @Override
  public void sendResourcePack(String url, byte[] hash) {
    Preconditions.checkNotNull(url, "url");
    Preconditions.checkNotNull(hash, "hash");
    Preconditions.checkArgument(hash.length == 20, "Hash length is not 20");

    if (this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      ResourcePackRequest request = new ResourcePackRequest();
      request.setUrl(url);
      request.setHash(ByteBufUtil.hexDump(hash));
      connection.write(request);
    }
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

  private class IdentityImpl implements Identity {
    @Override
    public @NonNull UUID uuid() {
      return ConnectedPlayer.this.getUniqueId();
    }
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
      Preconditions.checkArgument(server instanceof VelocityRegisteredServer,
          "Not a valid Velocity server.");
      if (connectionInFlight != null || (connectedServer != null
          && !connectedServer.hasCompletedJoin())) {
        return Optional.of(ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS);
      }
      if (connectedServer != null && connectedServer.getServer().equals(server)) {
        return Optional.of(ALREADY_CONNECTED);
      }
      return Optional.empty();
    }

    private CompletableFuture<Optional<Status>> getInitialStatus() {
      return CompletableFuture.supplyAsync(() -> checkServer(toConnect), connection.eventLoop());
    }

    private CompletableFuture<Impl> internalConnect() {
      return this.getInitialStatus()
          .thenCompose(initialCheck -> {
            if (initialCheck.isPresent()) {
              return completedFuture(plainResult(initialCheck.get(), toConnect));
            }

            ServerPreConnectEvent event = new ServerPreConnectEvent(ConnectedPlayer.this,
                toConnect);
            return server.getEventManager().fire(event)
                .thenComposeAsync(newEvent -> {
                  Optional<RegisteredServer> newDest = newEvent.getResult().getServer();
                  if (!newDest.isPresent()) {
                    return completedFuture(
                        plainResult(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED, toConnect)
                    );
                  }

                  RegisteredServer realDestination = newDest.get();
                  Optional<ConnectionRequestBuilder.Status> check = checkServer(realDestination);
                  if (check.isPresent()) {
                    return completedFuture(plainResult(check.get(), realDestination));
                  }

                  VelocityRegisteredServer vrs = (VelocityRegisteredServer) realDestination;
                  VelocityServerConnection con = new VelocityServerConnection(vrs,
                      ConnectedPlayer.this, server);
                  connectionInFlight = con;
                  return con.connect().whenCompleteAsync((result, throwable) ->
                      this.resetIfInFlightIs(con), connection.eventLoop());
                }, connection.eventLoop());
          });
    }

    private void resetIfInFlightIs(VelocityServerConnection establishedConnection) {
      if (establishedConnection == connectionInFlight) {
        resetInFlightConnection();
      }
    }

    @Override
    public CompletableFuture<Result> connect() {
      return this.internalConnect()
          .whenCompleteAsync((status, throwable) -> {
            if (status != null && !status.isSuccessful()) {
              if (!status.isSafe()) {
                handleConnectionException(status.getAttemptedConnection(), throwable, false);
              }
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
                sendMessage(Identity.nil(), ConnectionMessages.ALREADY_CONNECTED);
                break;
              case CONNECTION_IN_PROGRESS:
                sendMessage(Identity.nil(), ConnectionMessages.IN_PROGRESS);
                break;
              case CONNECTION_CANCELLED:
                // Ignored; the plugin probably already handled this.
                break;
              case SERVER_DISCONNECTED:
                Component reason = status.getReasonComponent()
                    .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
                handleConnectionException(toConnect, Disconnect.create(reason,
                    getProtocolVersion()), status.isSafe());
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
