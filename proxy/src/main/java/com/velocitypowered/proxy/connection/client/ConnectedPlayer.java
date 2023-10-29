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
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.network.connection.ServerConnection;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.player.Player;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import com.velocitypowered.proxy.network.pipeline.deser.MinecraftEncoder;
import com.velocitypowered.proxy.network.protocol.ProtocolUtils;
import com.velocitypowered.proxy.network.protocol.StateRegistry;
import com.velocitypowered.proxy.network.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.network.protocol.packet.Disconnect;
import com.velocitypowered.proxy.network.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.network.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.network.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.network.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.network.protocol.packet.chat.ChatQueue;
import com.velocitypowered.proxy.network.protocol.packet.chat.ChatType;
import com.velocitypowered.proxy.network.protocol.packet.chat.builder.ChatBuilderFactory;
import com.velocitypowered.proxy.network.protocol.packet.chat.legacy.LegacyChat;
import com.velocitypowered.proxy.network.protocol.packet.config.StartUpdate;
import com.velocitypowered.proxy.network.protocol.packet.title.GenericTitlePacket;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.tablist.InternalTabList;
import com.velocitypowered.proxy.tablist.KeyedVelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabListLegacy;
import com.velocitypowered.proxy.util.ClosestLocaleMatcher;
import com.velocitypowered.proxy.util.DurationUtils;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.platform.facet.FacetPointers.Type;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.KeybindComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;
import net.kyori.adventure.translation.GlobalTranslator;
import net.kyori.adventure.util.TriState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a player that is connected to the proxy.
 */
public class ConnectedPlayer implements MinecraftConnectionAssociation, Player, KeyIdentifiable,
    VelocityInboundConnection {

  private static final PlainTextComponentSerializer PASS_THRU_TRANSLATE =
      PlainTextComponentSerializer.builder()
          .flattener(ComponentFlattener.basic().toBuilder()
              .mapper(KeybindComponent.class, c -> "")
              .mapper(TranslatableComponent.class, TranslatableComponent::key)
              .build())
          .build();
  static final PermissionProvider DEFAULT_PERMISSIONS =
      s -> PermissionChecker.always(TriState.NOT_SET);

  private static final Logger logger = LogManager.getLogger(ConnectedPlayer.class);

  private final Identity identity = new IdentityImpl();
  /**
   * The actual Minecraft connection. This is actually a wrapper object around the Netty channel.
   */
  private final MinecraftConnection connection;
  private final @Nullable InetSocketAddress virtualHost;
  private GameProfile profile;
  private PermissionChecker permissionChecker;
  private int tryIndex = 0;
  private long ping = -1;
  private final boolean onlineMode;
  private @Nullable VelocityServerConnection connectedServer;
  private @Nullable VelocityServerConnection connectionInFlight;
  private @Nullable PlayerSettings settings;
  private @Nullable ModInfo modInfo;
  private Component playerListHeader = Component.empty();
  private Component playerListFooter = Component.empty();
  private final InternalTabList tabList;
  private final VelocityServer server;
  private ClientConnectionPhase connectionPhase;
  private final CompletableFuture<Void> teardownFuture = new CompletableFuture<>();
  private @MonotonicNonNull List<String> serversToTry = null;
  private @MonotonicNonNull Boolean previousResourceResponse;
  private final Queue<ResourcePackInfo> outstandingResourcePacks = new ArrayDeque<>();
  private @Nullable ResourcePackInfo pendingResourcePack;
  private @Nullable ResourcePackInfo appliedResourcePack;
  private final @NotNull Pointers pointers = Player.super.pointers().toBuilder()
      .withDynamic(Identity.UUID, this::uuid)
      .withDynamic(Identity.NAME, this::username)
      .withDynamic(Identity.DISPLAY_NAME, () -> Component.text(this.username()))
      .withDynamic(Identity.LOCALE, this::effectiveLocale)
      .withDynamic(PermissionChecker.POINTER, () -> this.permissionChecker)
      .withStatic(FacetPointers.TYPE, Type.PLAYER)
      .build();
  private @Nullable String clientBrand;
  private @Nullable Locale effectiveLocale;
  private @Nullable IdentifiedKey playerKey;
  private @Nullable ClientSettings clientSettingsPacket;
  private final ChatQueue chatQueue;
  private final ChatBuilderFactory chatBuilderFactory;

  ConnectedPlayer(VelocityServer server, GameProfile profile, MinecraftConnection connection,
      @Nullable InetSocketAddress virtualHost, boolean onlineMode,
      @Nullable IdentifiedKey playerKey) {
    this.server = server;
    this.profile = profile;
    this.connection = connection;
    this.virtualHost = virtualHost;
    this.permissionChecker = PermissionChecker.always(TriState.NOT_SET);
    this.connectionPhase = connection.getType().getInitialClientPhase();
    this.onlineMode = onlineMode;

    if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      this.tabList = new VelocityTabList(this);
    } else if (connection.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.tabList = new KeyedVelocityTabList(this, server);
    } else {
      this.tabList = new VelocityTabListLegacy(this, server);
    }
    this.playerKey = playerKey;
    this.chatQueue = new ChatQueue(this);
    this.chatBuilderFactory = new ChatBuilderFactory(this.protocolVersion());
  }

  public ChatBuilderFactory getChatBuilderFactory() {
    return chatBuilderFactory;
  }

  public ChatQueue getChatQueue() {
    return chatQueue;
  }

  @Override
  public @NonNull Identity identity() {
    return this.identity;
  }

  @Override
  public String username() {
    return profile.name();
  }

  @Override
  public Locale effectiveLocale() {
    if (effectiveLocale == null && settings != null) {
      return settings.locale();
    }
    return effectiveLocale;
  }

  @Override
  public void setEffectiveLocale(Locale locale) {
    effectiveLocale = locale;
  }

  @Override
  public UUID uuid() {
    return profile.uuid();
  }

  @Override
  public Optional<ServerConnection> connectedServer() {
    return Optional.ofNullable(connectedServer);
  }

  /**
   * Makes sure the player is connected to a server and returns the server they are connected to.
   *
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
  public GameProfile profile() {
    return profile;
  }

  public MinecraftConnection getConnection() {
    return connection;
  }

  @Override
  public long ping() {
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
  public PlayerSettings settings() {
    return settings == null ? ClientSettingsWrapper.DEFAULT : this.settings;
  }

  public ClientSettings getClientSettingsPacket() {
    return clientSettingsPacket;
  }

  @Override
  public boolean hasSentPlayerSettings() {
    return settings != null;
  }

  /**
   * Sets player settings.
   *
   * @param clientSettingsPacket the player settings packet
   */
  public void setClientSettings(final ClientSettings clientSettingsPacket) {
    this.clientSettingsPacket = clientSettingsPacket;
    final ClientSettingsWrapper cs = new ClientSettingsWrapper(clientSettingsPacket);
    this.settings = cs;
    server.eventManager().fireAndForget(new PlayerSettingsChangedEvent(this, cs));
  }

  @Override
  public Optional<ModInfo> modInfo() {
    return Optional.ofNullable(modInfo);
  }

  public void setModInfo(ModInfo modInfo) {
    this.modInfo = modInfo;
    server.eventManager().fireAndForget(new PlayerModInfoEvent(this, modInfo));
  }

  @Override
  public @NotNull Pointers pointers() {
    return this.pointers;
  }

  @Override
  public SocketAddress remoteAddress() {
    return connection.getRemoteAddress();
  }

  @Override
  public Optional<InetSocketAddress> virtualHost() {
    return Optional.ofNullable(virtualHost);
  }

  void setPermissionChecker(PermissionChecker permissionChecker) {
    this.permissionChecker = permissionChecker;
  }

  @Override
  public boolean isActive() {
    return connection.getChannel().isActive();
  }

  @Override
  public ProtocolVersion protocolVersion() {
    return connection.getProtocolVersion();
  }

  /**
   * Translates the message in the user's locale.
   *
   * @param message the message to translate
   * @return the translated message
   */
  public Component translateMessage(Component message) {
    Locale locale = ClosestLocaleMatcher.INSTANCE
        .lookupClosest(effectiveLocale() == null ? Locale.getDefault() : effectiveLocale());
    return GlobalTranslator.render(message, locale);
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message) {
    Component translated = translateMessage(message);

    connection.write(getChatBuilderFactory().builder()
        .component(translated).forIdentity(identity).toClient());
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message,
      @NonNull MessageType type) {
    Preconditions.checkNotNull(message, "message");
    Preconditions.checkNotNull(type, "type");

    Component translated = translateMessage(message);

    connection.write(getChatBuilderFactory().builder()
        .component(translated).forIdentity(identity)
        .setType(type == MessageType.CHAT ? ChatType.CHAT : ChatType.SYSTEM)
        .toClient());
  }

  @Override
  public void sendActionBar(net.kyori.adventure.text.@NonNull Component message) {
    Component translated = translateMessage(message);

    ProtocolVersion playerVersion = protocolVersion();
    if (playerVersion.compareTo(ProtocolVersion.MINECRAFT_1_11) >= 0) {
      // Use the title packet instead.
      GenericTitlePacket pkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_ACTION_BAR, playerVersion);
      pkt.setComponent(ProtocolUtils.getJsonChatSerializer(playerVersion)
          .serialize(translated));
      connection.write(pkt);
    } else {
      // Due to issues with action bar packets, we'll need to convert the text message into a
      // legacy message and then inject the legacy text into a component... yuck!
      JsonObject object = new JsonObject();
      object.addProperty("text", LegacyComponentSerializer.legacySection()
          .serialize(translated));
      LegacyChat legacyChat = new LegacyChat();
      legacyChat.setMessage(object.toString());
      legacyChat.setType(LegacyChat.GAME_INFO_TYPE);
      connection.write(legacyChat);
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
    Component translatedHeader = translateMessage(header);
    Component translatedFooter = translateMessage(footer);
    this.playerListHeader = translatedHeader;
    this.playerListFooter = translatedFooter;
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.connection.write(HeaderAndFooter.create(header, footer, this.protocolVersion()));
    }
  }

  @Override
  public void showTitle(net.kyori.adventure.title.@NonNull Title title) {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(this
          .protocolVersion());
      GenericTitlePacket timesPkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TIMES, this.protocolVersion());
      net.kyori.adventure.title.Title.Times times = title.times();
      if (times != null) {
        timesPkt.setFadeIn((int) DurationUtils.toTicks(times.fadeIn()));
        timesPkt.setStay((int) DurationUtils.toTicks(times.stay()));
        timesPkt.setFadeOut((int) DurationUtils.toTicks(times.fadeOut()));
      }
      connection.delayedWrite(timesPkt);

      GenericTitlePacket subtitlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_SUBTITLE, this.protocolVersion());
      subtitlePkt.setComponent(serializer.serialize(translateMessage(title.subtitle())));
      connection.delayedWrite(subtitlePkt);

      GenericTitlePacket titlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TITLE, this.protocolVersion());
      titlePkt.setComponent(serializer.serialize(translateMessage(title.title())));
      connection.delayedWrite(titlePkt);

      connection.flush();
    }
  }

  @Override
  public <T> void sendTitlePart(@NotNull TitlePart<T> part, @NotNull T value) {
    if (part == null) {
      throw new NullPointerException("part");
    }
    if (value == null) {
      throw new NullPointerException("value");
    }

    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) < 0) {
      return;
    }

    GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(this
        .protocolVersion());

    if (part == TitlePart.TITLE) {
      GenericTitlePacket titlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TITLE, this.protocolVersion());
      titlePkt.setComponent(serializer.serialize(translateMessage((Component) value)));
      connection.write(titlePkt);
    } else if (part == TitlePart.SUBTITLE) {
      GenericTitlePacket titlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_SUBTITLE, this.protocolVersion());
      titlePkt.setComponent(serializer.serialize(translateMessage((Component) value)));
      connection.write(titlePkt);
    } else if (part == TitlePart.TIMES) {
      Times times = (Times) value;
      GenericTitlePacket timesPkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TIMES, this.protocolVersion());
      timesPkt.setFadeIn((int) DurationUtils.toTicks(times.fadeIn()));
      timesPkt.setStay((int) DurationUtils.toTicks(times.stay()));
      timesPkt.setFadeOut((int) DurationUtils.toTicks(times.fadeOut()));
      connection.write(timesPkt);
    } else {
      throw new IllegalArgumentException("Title part " + part + " is not valid");
    }
  }

  @Override
  public void clearTitle() {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.HIDE, this.protocolVersion()));
    }
  }

  @Override
  public void resetTitle() {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.RESET, this.protocolVersion()));
    }
  }

  @Override
  public void hideBossBar(@NonNull BossBar bar) {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      this.server.getBossBarManager().removeBossBar(this, bar);
    }
  }

  @Override
  public void showBossBar(@NonNull BossBar bar) {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_9) >= 0) {
      this.server.getBossBarManager().addBossBar(this, bar);
    }
  }

  @Override
  public ConnectionRequestBuilder createConnectionRequest(RegisteredServer server) {
    return new ConnectionRequestBuilderImpl(server, this.connectedServer);
  }

  private ConnectionRequestBuilder createConnectionRequest(RegisteredServer server,
      @Nullable VelocityServerConnection previousConnection) {
    return new ConnectionRequestBuilderImpl(server, previousConnection);
  }

  @Override
  public List<GameProfile.Property> profileProperties() {
    return this.profile.properties();
  }

  @Override
  public void setProfileProperties(List<GameProfile.Property> properties) {
    this.profile = profile.withProperties(Preconditions.checkNotNull(properties));
  }

  @Override
  public void clearPlayerListHeaderAndFooter() {
    clearPlayerListHeaderAndFooterSilent();
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      this.connection.write(HeaderAndFooter.reset());
    }
  }

  public void clearPlayerListHeaderAndFooterSilent() {
    this.playerListHeader = Component.empty();
    this.playerListFooter = Component.empty();
  }

  @Override
  public InternalTabList tabList() {
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

  /**
   * Disconnects the player from the proxy.
   *
   * @param reason      the reason for disconnecting the player
   * @param duringLogin whether the disconnect happened during login
   */
  public void disconnect0(Component reason, boolean duringLogin) {
    Component translated = this.translateMessage(reason);

    if (server.configuration().isLogPlayerConnections()) {
      logger.info("{} has disconnected: {}", this,
          LegacyComponentSerializer.legacySection().serialize(translated));
    }
    connection.closeWith(Disconnect.create(translated, this.protocolVersion()));
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
   *
   * @param server    the server we disconnected from
   * @param throwable the exception
   * @param safe      whether or not we can safely reconnect to a new server
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

    Component friendlyError;
    if (connectedServer != null && connectedServer.serverInfo().equals(server.serverInfo())) {
      friendlyError = Component.translatable("velocity.error.connected-server-error",
          Component.text(server.serverInfo().name()));
    } else {
      logger.error("{}: unable to connect to server {}", this, server.serverInfo().name(),
          wrapped);
      friendlyError = Component.translatable("velocity.error.connecting-server-error",
          Component.text(server.serverInfo().name()));
    }
    handleConnectionException(server, null, friendlyError.color(NamedTextColor.RED), safe);
  }

  /**
   * Handles unexpected disconnects.
   *
   * @param server     the server we disconnected from
   * @param disconnect the disconnect packet
   * @param safe       whether or not we can safely reconnect to a new server
   */
  public void handleConnectionException(RegisteredServer server, Disconnect disconnect,
      boolean safe) {
    if (!isActive()) {
      // If the connection is no longer active, it makes no sense to try and recover it.
      return;
    }

    Component disconnectReason = GsonComponentSerializer.gson().deserialize(disconnect.getReason());
    String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
    if (connectedServer != null && connectedServer.serverInfo().equals(server.serverInfo())) {
      logger.info("{}: kicked from server {}: {}", this, server.serverInfo().name(),
          plainTextReason);
      handleConnectionException(server, disconnectReason,
          Component.translatable("velocity.error.moved-to-new-server", NamedTextColor.RED,
              Component.text(server.serverInfo().name()),
              disconnectReason), safe);
    } else {
      logger.error("{}: disconnected while connecting to {}: {}", this,
          server.serverInfo().name(), plainTextReason);
      handleConnectionException(server, disconnectReason,
          Component.translatable("velocity.error.cant-connect", NamedTextColor.RED,
              Component.text(server.serverInfo().name()),
              disconnectReason), safe);
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

    boolean kickedFromCurrent = connectedServer == null || connectedServer.server().equals(rs);
    ServerKickResult result;
    if (kickedFromCurrent) {
      Optional<RegisteredServer> next = getNextServerToTry(rs);
      result = next.map(RedirectPlayer::redirect)
          .orElseGet(() -> DisconnectPlayer.disconnect(friendlyReason));
    } else {
      // If we were kicked by going to another server, the connection should not be in flight
      if (connectionInFlight != null && connectionInFlight.server().equals(rs)) {
        resetInFlightConnection();
      }
      result = Notify.notify(friendlyReason);
    }
    KickedFromServerEvent originalEvent = new KickedFromServerEvent(this, rs, kickReason,
        !kickedFromCurrent, result);
    handleKickEvent(originalEvent, friendlyReason, kickedFromCurrent);
  }

  private void handleKickEvent(KickedFromServerEvent originalEvent, Component friendlyReason,
      boolean kickedFromCurrent) {
    server.eventManager().fire(originalEvent).thenAcceptAsync(event -> {
      // There can't be any connection in flight now.
      connectionInFlight = null;

      // Make sure we clear the current connected server as the connection is invalid.
      VelocityServerConnection previousConnection = connectedServer;
      if (kickedFromCurrent) {
        connectedServer = null;
      }

      if (!isActive()) {
        // If the connection is no longer active, it makes no sense to try and recover it.
        return;
      }

      if (event.result() instanceof DisconnectPlayer) {
        DisconnectPlayer res = (DisconnectPlayer) event.result();
        disconnect(res.reason());
      } else if (event.result() instanceof RedirectPlayer) {
        RedirectPlayer res = (RedirectPlayer) event.result();
        createConnectionRequest(res.getServer(), previousConnection).connect()
            .whenCompleteAsync((status, throwable) -> {
              if (throwable != null) {
                handleConnectionException(
                    status != null ? status.attemptedConnectedTo() : res.getServer(), throwable,
                    true);
                return;
              }

              switch (status.status()) {
                // Impossible/nonsensical cases
                case ALREADY_CONNECTED:
                  logger.error("{}: already connected to {}", this,
                      status.attemptedConnectedTo().serverInfo().name());
                  break;
                case CONNECTION_IN_PROGRESS:
                  // Fatal case
                case CONNECTION_CANCELLED:
                  Component fallbackMsg = res.getMessageComponent();
                  if (fallbackMsg == null) {
                    fallbackMsg = friendlyReason;
                  }
                  disconnect(status.reason().orElse(fallbackMsg));
                  break;
                case SERVER_DISCONNECTED:
                  Component reason = status.reason()
                      .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
                  handleConnectionException(res.getServer(),
                      Disconnect.create(reason, protocolVersion()), ((Impl) status).isSafe());
                  break;
                case SUCCESS:
                  Component requestedMessage = res.getMessageComponent();
                  if (requestedMessage == null) {
                    requestedMessage = friendlyReason;
                  }
                  if (requestedMessage != Component.empty()) {
                    sendMessage(requestedMessage);
                  }
                  break;
                default:
                  // The only remaining value is successful (no need to do anything!)
                  break;
              }
            }, connection.eventLoop());
      } else if (event.result() instanceof Notify) {
        Notify res = (Notify) event.result();
        if (event.kickedDuringServerConnect() && previousConnection != null) {
          sendMessage(Identity.nil(), res.reason());
        } else {
          disconnect(res.reason());
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
   * @return the next server to try
   */
  private Optional<RegisteredServer> getNextServerToTry(@Nullable RegisteredServer current) {
    if (serversToTry == null) {
      String virtualHostStr = virtualHost().map(InetSocketAddress::getHostString)
          .orElse("")
          .toLowerCase(Locale.ROOT);
      serversToTry = server.configuration().getForcedHosts().getOrDefault(virtualHostStr,
          Collections.emptyList());
    }

    if (serversToTry.isEmpty()) {
      List<String> connOrder = server.configuration().getAttemptConnectionOrder();
      if (connOrder.isEmpty()) {
        return Optional.empty();
      } else {
        serversToTry = connOrder;
      }
    }

    for (int i = tryIndex; i < serversToTry.size(); i++) {
      String toTryName = serversToTry.get(i);
      if ((connectedServer != null && hasSameName(connectedServer.server(), toTryName))
          || (connectionInFlight != null && hasSameName(connectionInFlight.server(), toTryName))
          || (current != null && hasSameName(current, toTryName))) {
        continue;
      }

      tryIndex = i;
      return server.server(toTryName);
    }
    return Optional.empty();
  }

  private static boolean hasSameName(RegisteredServer server, String name) {
    return server.serverInfo().name().equalsIgnoreCase(name);
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

    Optional<Player> connectedPlayer = server.player(this.uuid());
    server.unregisterConnection(this);

    DisconnectEvent.LoginStatus status;
    if (connectedPlayer.isPresent()) {
      if (!connectedPlayer.get().connectedServer().isPresent()) {
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
    server.eventManager().fire(event).whenComplete((val, ex) -> {
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
    boolean isPlayerAddressLoggingEnabled = server.configuration()
        .isPlayerAddressLoggingEnabled();
    String playerIp =
        isPlayerAddressLoggingEnabled ? remoteAddress().toString() : "<ip address withheld>";
    return "[connected player] " + profile.name() + " (" + playerIp + ")";
  }

  @Override
  public PermissionChecker permissionChecker() {
    return this.permissionChecker;
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");
    PluginMessage message = new PluginMessage(identifier.id(), Unpooled.wrappedBuffer(data));
    connection.write(message);
    return true;
  }

  @Override
  public String getClientBrand() {
    return clientBrand;
  }

  void setClientBrand(String clientBrand) {
    this.clientBrand = clientBrand;
  }

  @Override
  public void spoofChatInput(String input) {
    Preconditions.checkArgument(input.length() <= LegacyChat.MAX_SERVERBOUND_MESSAGE_LENGTH,
        "input cannot be greater than " + LegacyChat.MAX_SERVERBOUND_MESSAGE_LENGTH
            + " characters in length");
    if (protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      this.chatQueue.hijack(getChatBuilderFactory().builder().asPlayer(this).message(input),
          (instant, item) -> {
            item.setTimestamp(instant);
            return item.toServer();
          });
    } else {
      ensureBackendConnection().write(getChatBuilderFactory().builder()
          .asPlayer(this).message(input).toServer());
    }
  }

  @Override
  public void sendResourcePackOffer(ResourcePackInfo packInfo) {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      Preconditions.checkNotNull(packInfo, "packInfo");
      queueResourcePack(packInfo);
    }
  }

  /**
   * Queues a resource-pack for sending to the player and sends it immediately if the queue is
   * empty.
   */
  public void queueResourcePack(ResourcePackInfo info) {
    outstandingResourcePacks.add(info);
    if (outstandingResourcePacks.size() == 1) {
      tickResourcePackQueue();
    }
  }

  private void tickResourcePackQueue() {
    ResourcePackInfo queued = outstandingResourcePacks.peek();

    if (queued != null) {
      // Check if the player declined a resource pack once already
      if (previousResourceResponse != null && !previousResourceResponse) {
        // If that happened we can flush the queue right away.
        // Unless its 1.17+ and forced it will come back denied anyway
        while (!outstandingResourcePacks.isEmpty()) {
          queued = outstandingResourcePacks.peek();
          if (queued.required() && protocolVersion()
              .compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0) {
            break;
          }
          onResourcePackResponse(PlayerResourcePackStatusEvent.Status.DECLINED);
          queued = null;
        }
        if (queued == null) {
          // Exit as the queue was cleared
          return;
        }
      }

      ResourcePackRequest request = new ResourcePackRequest();
      request.setUrl(queued.url());
      if (queued.hash() != null) {
        request.setHash(ByteBufUtil.hexDump(queued.hash()));
      } else {
        request.setHash("");
      }
      request.setRequired(queued.required());
      request.setPrompt(queued.prompt());

      connection.write(request);
    }
  }

  @Override
  public @Nullable ResourcePackInfo appliedResourcePack() {
    return appliedResourcePack;
  }

  @Override
  public @Nullable ResourcePackInfo pendingResourcePack() {
    return pendingResourcePack;
  }

  /**
   * Clears the applied resource pack field.
   */
  public void clearAppliedResourcePack() {
    appliedResourcePack = null;
  }

  /**
   * Processes a client response to a sent resource-pack.
   */
  public boolean onResourcePackResponse(PlayerResourcePackStatusEvent.Status status) {
    final boolean peek = status == PlayerResourcePackStatusEvent.Status.ACCEPTED;
    final ResourcePackInfo queued = peek
        ? outstandingResourcePacks.peek() : outstandingResourcePacks.poll();

    server.eventManager().fire(new PlayerResourcePackStatusEvent(this, status, queued))
        .thenAcceptAsync(event -> {
          if (event.getStatus() == PlayerResourcePackStatusEvent.Status.DECLINED
              && event.getPackInfo() != null && event.getPackInfo().required()
              && (!event.isOverwriteKick() || event.player()
              .protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_17) >= 0)
          ) {
            event.player().disconnect(Component
                .translatable("multiplayer.requiredTexturePrompt.disconnect"));
          }
        });

    switch (status) {
      case ACCEPTED:
        previousResourceResponse = true;
        pendingResourcePack = queued;
        break;
      case DECLINED:
        previousResourceResponse = false;
        break;
      case SUCCESSFUL:
        appliedResourcePack = queued;
        pendingResourcePack = null;
        break;
      case FAILED_DOWNLOAD:
        pendingResourcePack = null;
        break;
      default:
        break;
    }

    if (!peek) {
      connection.eventLoop().execute(this::tickResourcePackQueue);
    }

    return queued != null
        && queued.originalOrigin() != ResourcePackInfo.Origin.DOWNSTREAM_SERVER;
  }

  /**
   * Gives an indication about the previous resource pack responses.
   */
  public @Nullable Boolean getPreviousResourceResponse() {
    return previousResourceResponse;
  }

  /**
   * Sends a {@link KeepAlive} packet to the player with a random ID. The response will be ignored
   * by Velocity as it will not match the ID last sent by the server.
   */
  public void sendKeepAlive() {
    if (connection.getState() == StateRegistry.PLAY
        || connection.getState() == StateRegistry.CONFIG) {
      KeepAlive keepAlive = new KeepAlive();
      keepAlive.setRandomId(ThreadLocalRandom.current().nextLong());
      connection.write(keepAlive);
    }
  }

  /**
   * Switches the connection to the client into config state.
   */
  public void switchToConfigState() {
    CompletableFuture.runAsync(() -> {
      connection.write(new StartUpdate());
      connection.getChannel().pipeline()
          .get(MinecraftEncoder.class).setState(StateRegistry.CONFIG);
      // Make sure we don't send any play packets to the player after update start
      connection.addPlayPacketQueueHandler();
    }, connection.eventLoop()).exceptionally((ex) -> {
      logger.error("Error switching player connection to config state:", ex);
      return null;
    });
  }

  /**
   * Gets the current "phase" of the connection, mostly used for tracking modded negotiation for
   * legacy forge servers and provides methods for performing phase specific actions.
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

  @Override
  public @Nullable IdentifiedKey identifiedKey() {
    return playerKey;
  }

  private class IdentityImpl implements Identity {

    @Override
    public @NonNull UUID uuid() {
      return ConnectedPlayer.this.uuid();
    }
  }

  private class ConnectionRequestBuilderImpl implements ConnectionRequestBuilder {

    private final RegisteredServer toConnect;
    private final @Nullable VelocityRegisteredServer previousServer;

    ConnectionRequestBuilderImpl(RegisteredServer toConnect,
        @Nullable VelocityServerConnection previousConnection) {
      this.toConnect = Preconditions.checkNotNull(toConnect, "info");
      this.previousServer = previousConnection == null ? null : previousConnection.server();
    }

    @Override
    public RegisteredServer server() {
      return toConnect;
    }

    private Optional<ConnectionRequestBuilder.Status> checkServer(RegisteredServer server) {
      Preconditions.checkArgument(server instanceof VelocityRegisteredServer,
          "Not a valid Velocity server.");
      if (connectionInFlight != null || (connectedServer != null
          && !connectedServer.hasCompletedJoin())) {
        return Optional.of(ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS);
      }
      if (connectedServer != null
          && connectedServer.server().serverInfo().equals(server.serverInfo())) {
        return Optional.of(ALREADY_CONNECTED);
      }
      return Optional.empty();
    }

    private CompletableFuture<Optional<Status>> getInitialStatus() {
      return CompletableFuture.supplyAsync(() -> checkServer(toConnect), connection.eventLoop());
    }

    private CompletableFuture<Impl> internalConnect() {
      return this.getInitialStatus().thenCompose(initialCheck -> {
        if (initialCheck.isPresent()) {
          return completedFuture(plainResult(initialCheck.get(), toConnect));
        }

        ServerPreConnectEvent event =
            new ServerPreConnectEvent(ConnectedPlayer.this, toConnect, previousServer);
        return server.eventManager().fire(event).thenComposeAsync(newEvent -> {
          Optional<RegisteredServer> newDest = newEvent.result().getServer();
          if (!newDest.isPresent()) {
            return completedFuture(
                plainResult(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED, toConnect));
          }

          RegisteredServer realDestination = newDest.get();
          Optional<ConnectionRequestBuilder.Status> check = checkServer(realDestination);
          if (check.isPresent()) {
            return completedFuture(plainResult(check.get(), realDestination));
          }

          VelocityRegisteredServer vrs = (VelocityRegisteredServer) realDestination;
          VelocityServerConnection con =
              new VelocityServerConnection(vrs, previousServer, ConnectedPlayer.this, server);
          connectionInFlight = con;
          return con.connect().whenCompleteAsync((result, exception) -> this.resetIfInFlightIs(con),
              connection.eventLoop());
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
      return this.internalConnect().whenCompleteAsync((status, throwable) -> {
        if (status != null && !status.isSuccessful()) {
          if (!status.isSafe()) {
            handleConnectionException(status.attemptedConnectedTo(), throwable, false);
          }
        }
      }, connection.eventLoop()).thenApply(x -> x);
    }

    @Override
    public CompletableFuture<Boolean> connectWithIndication() {
      return internalConnect().whenCompleteAsync((status, throwable) -> {
        if (throwable != null) {
          // TODO: The exception handling from this is not very good. Find a better way.
          handleConnectionException(status != null ? status.attemptedConnectedTo() : toConnect,
              throwable, true);
          return;
        }

        switch (status.status()) {
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
            Component reason = status.reason()
                .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
            handleConnectionException(toConnect, Disconnect.create(reason, protocolVersion()),
                status.isSafe());
            break;
          default:
            // The only remaining value is successful (no need to do anything!)
            break;
        }
      }, connection.eventLoop()).thenApply(Result::isSuccessful);
    }

    @Override
    public void fireAndForget() {
      connectWithIndication();
    }
  }
}
