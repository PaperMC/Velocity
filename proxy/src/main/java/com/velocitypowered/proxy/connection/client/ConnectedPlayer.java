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
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.connection.PreTransferEvent;
import com.velocitypowered.api.event.player.CookieRequestEvent;
import com.velocitypowered.api.event.player.CookieStoreEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.Notify;
import com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.ServerKickResult;
import com.velocitypowered.api.event.player.PlayerModInfoEvent;
import com.velocitypowered.api.event.player.PlayerSettingsChangedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.crypto.KeyIdentifiable;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.PluginMessageEncoder;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.adventure.VelocityBossBarImplementation;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.connection.player.resourcepack.ResourcePackHandler;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.connection.util.VelocityInboundConnection;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundCookieRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ClientboundStoreCookiePacket;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooterPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.RemoveResourcePackPacket;
import com.velocitypowered.proxy.protocol.packet.TransferPacket;
import com.velocitypowered.proxy.protocol.packet.chat.ChatQueue;
import com.velocitypowered.proxy.protocol.packet.chat.ChatType;
import com.velocitypowered.proxy.protocol.packet.chat.ComponentHolder;
import com.velocitypowered.proxy.protocol.packet.chat.PlayerChatCompletionPacket;
import com.velocitypowered.proxy.protocol.packet.chat.builder.ChatBuilderFactory;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatPacket;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import com.velocitypowered.proxy.protocol.util.ByteBufDataOutput;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.tablist.InternalTabList;
import com.velocitypowered.proxy.tablist.KeyedVelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabListLegacy;
import com.velocitypowered.proxy.util.ClosestLocaleMatcher;
import com.velocitypowered.proxy.util.DurationUtils;
import com.velocitypowered.proxy.util.TranslatableMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.audience.MessageType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.permission.PermissionChecker;
import net.kyori.adventure.platform.facet.FacetPointers;
import net.kyori.adventure.platform.facet.FacetPointers.Type;
import net.kyori.adventure.pointer.Pointers;
import net.kyori.adventure.resource.ResourcePackInfoLike;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.resource.ResourcePackRequestLike;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.title.TitlePart;
import net.kyori.adventure.translation.GlobalTranslator;
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
      PlainTextComponentSerializer.builder().flattener(TranslatableMapper.FLATTENER).build();
  static final PermissionProvider DEFAULT_PERMISSIONS = s -> PermissionFunction.ALWAYS_UNDEFINED;

  private static final ComponentLogger logger = ComponentLogger.logger(ConnectedPlayer.class);

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
  private final Set<VelocityBossBarImplementation> bossBars = new HashSet<>();
  private Component playerListHeader = Component.empty();
  private Component playerListFooter = Component.empty();
  private final InternalTabList tabList;
  private final VelocityServer server;
  private ClientConnectionPhase connectionPhase;
  private final CompletableFuture<Void> teardownFuture = new CompletableFuture<>();
  private @MonotonicNonNull List<String> serversToTry = null;
  private final ResourcePackHandler resourcePackHandler;
  private final BundleDelimiterHandler bundleHandler = new BundleDelimiterHandler(this);

  private final @NotNull Pointers pointers =
      Player.super.pointers().toBuilder()
          .withDynamic(Identity.UUID, this::getUniqueId)
          .withDynamic(Identity.NAME, this::getUsername)
          .withDynamic(Identity.DISPLAY_NAME, () -> Component.text(this.getUsername()))
          .withDynamic(Identity.LOCALE, this::getEffectiveLocale)
          .withStatic(PermissionChecker.POINTER, getPermissionChecker())
          .withStatic(FacetPointers.TYPE, Type.PLAYER).build();
  private @Nullable String clientBrand;
  private @Nullable Locale effectiveLocale;
  private final @Nullable IdentifiedKey playerKey;
  private @Nullable ClientSettingsPacket clientSettingsPacket;
  private final ChatQueue chatQueue;
  private final ChatBuilderFactory chatBuilderFactory;

  ConnectedPlayer(VelocityServer server, GameProfile profile, MinecraftConnection connection,
                  @Nullable InetSocketAddress virtualHost, boolean onlineMode,
                  @Nullable IdentifiedKey playerKey) {
    this.server = server;
    this.profile = profile;
    this.connection = connection;
    this.virtualHost = virtualHost;
    this.permissionFunction = PermissionFunction.ALWAYS_UNDEFINED;
    this.connectionPhase = connection.getType().getInitialClientPhase();
    this.onlineMode = onlineMode;

    if (connection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      this.tabList = new VelocityTabList(this);
    } else if (connection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      this.tabList = new KeyedVelocityTabList(this, server);
    } else {
      this.tabList = new VelocityTabListLegacy(this, server);
    }
    this.playerKey = playerKey;
    this.chatQueue = new ChatQueue(this);
    this.chatBuilderFactory = new ChatBuilderFactory(this.getProtocolVersion());
    this.resourcePackHandler = ResourcePackHandler.create(this, server);
  }

  /**
   * Used for cleaning up resources during a disconnection.
   */
  public void disconnected() {
    for (final VelocityBossBarImplementation bar : this.bossBars) {
      bar.viewerDisconnected(this);
    }
  }

  public ChatBuilderFactory getChatBuilderFactory() {
    return chatBuilderFactory;
  }

  public ChatQueue getChatQueue() {
    return chatQueue;
  }

  public BundleDelimiterHandler getBundleHandler() {
    return this.bundleHandler;
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
  public Locale getEffectiveLocale() {
    if (effectiveLocale == null && settings != null) {
      return settings.getLocale();
    }
    return effectiveLocale;
  }

  @Override
  public void setEffectiveLocale(final @Nullable Locale locale) {
    effectiveLocale = locale;
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

  @Nullable
  public ClientSettingsPacket getClientSettingsPacket() {
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
  public void setClientSettings(final ClientSettingsPacket clientSettingsPacket) {
    this.clientSettingsPacket = clientSettingsPacket;
    final ClientSettingsWrapper cs = new ClientSettingsWrapper(clientSettingsPacket);
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
  public @NotNull Pointers pointers() {
    return this.pointers;
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

  /**
   * Translates the message in the user's locale.
   *
   * @param message the message to translate
   * @return the translated message
   */
  public Component translateMessage(Component message) {
    Locale locale = ClosestLocaleMatcher.INSTANCE
        .lookupClosest(getEffectiveLocale() == null ? Locale.getDefault() : getEffectiveLocale());
    return GlobalTranslator.render(message, locale);
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message) {
    final Component translated = translateMessage(message);

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

    ProtocolVersion playerVersion = getProtocolVersion();
    if (playerVersion.noLessThan(ProtocolVersion.MINECRAFT_1_11)) {
      // Use the title packet instead.
      GenericTitlePacket pkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_ACTION_BAR, playerVersion);
      pkt.setComponent(new ComponentHolder(playerVersion, translated));
      connection.write(pkt);
    } else {
      // Due to issues with action bar packets, we'll need to convert the text message into a
      // legacy message and then inject the legacy text into a component... yuck!
      JsonObject object = new JsonObject();
      object.addProperty("text", LegacyComponentSerializer.legacySection()
          .serialize(translated));
      LegacyChatPacket legacyChat = new LegacyChatPacket();
      legacyChat.setMessage(object.toString());
      legacyChat.setType(LegacyChatPacket.GAME_INFO_TYPE);
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
  public void sendPlayerListHeaderAndFooter(final @NotNull Component header,
                                            final @NotNull Component footer) {
    Component translatedHeader = translateMessage(header);
    Component translatedFooter = translateMessage(footer);
    this.playerListHeader = translatedHeader;
    this.playerListFooter = translatedFooter;
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      this.connection.write(HeaderAndFooterPacket.create(
          translatedHeader, translatedFooter, this.getProtocolVersion()));
    }
  }

  @Override
  public void showTitle(net.kyori.adventure.title.@NonNull Title title) {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      GenericTitlePacket timesPkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TIMES, this.getProtocolVersion());
      net.kyori.adventure.title.Title.Times times = title.times();
      if (times != null) {
        timesPkt.setFadeIn((int) DurationUtils.toTicks(times.fadeIn()));
        timesPkt.setStay((int) DurationUtils.toTicks(times.stay()));
        timesPkt.setFadeOut((int) DurationUtils.toTicks(times.fadeOut()));
      }
      connection.delayedWrite(timesPkt);

      GenericTitlePacket subtitlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_SUBTITLE, this.getProtocolVersion());
      subtitlePkt.setComponent(new ComponentHolder(
          this.getProtocolVersion(), translateMessage(title.subtitle())));
      connection.delayedWrite(subtitlePkt);

      GenericTitlePacket titlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TITLE, this.getProtocolVersion());
      titlePkt.setComponent(new ComponentHolder(
          this.getProtocolVersion(), translateMessage(title.title())));
      connection.delayedWrite(titlePkt);

      connection.flush();
    }
  }

  @SuppressWarnings("ConstantValue")
  @Override
  public <T> void sendTitlePart(@NotNull TitlePart<T> part, @NotNull T value) {
    if (part == null) {
      throw new NullPointerException("part");
    }
    if (value == null) {
      throw new NullPointerException("value");
    }

    if (this.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_8)) {
      return;
    }

    if (part == TitlePart.TITLE) {
      GenericTitlePacket titlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TITLE, this.getProtocolVersion());
      titlePkt.setComponent(new ComponentHolder(
          this.getProtocolVersion(), translateMessage((Component) value)));
      connection.write(titlePkt);
    } else if (part == TitlePart.SUBTITLE) {
      GenericTitlePacket titlePkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_SUBTITLE, this.getProtocolVersion());
      titlePkt.setComponent(new ComponentHolder(
          this.getProtocolVersion(), translateMessage((Component) value)));
      connection.write(titlePkt);
    } else if (part == TitlePart.TIMES) {
      Times times = (Times) value;
      GenericTitlePacket timesPkt = GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.SET_TIMES, this.getProtocolVersion());
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
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      connection.write(GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.HIDE, this.getProtocolVersion()));
    }
  }

  @Override
  public void resetTitle() {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      connection.write(GenericTitlePacket.constructTitlePacket(
          GenericTitlePacket.ActionType.RESET, this.getProtocolVersion()));
    }
  }

  @Override
  public void hideBossBar(@NonNull BossBar bar) {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
      final VelocityBossBarImplementation impl = VelocityBossBarImplementation.get(bar);
      if (impl.viewerRemove(this)) {
        this.bossBars.remove(impl);
      }
    }
  }

  @Override
  public void showBossBar(@NonNull BossBar bar) {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_9)) {
      final VelocityBossBarImplementation impl = VelocityBossBarImplementation.get(bar);
      if (impl.viewerAdd(this)) {
        this.bossBars.add(impl);
      }
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
  public List<GameProfile.Property> getGameProfileProperties() {
    return this.profile.getProperties();
  }

  @Override
  public void setGameProfileProperties(List<GameProfile.Property> properties) {
    this.profile = profile.withProperties(Preconditions.checkNotNull(properties));
  }

  @Override
  public void clearPlayerListHeaderAndFooter() {
    clearPlayerListHeaderAndFooterSilent();
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      this.connection.write(HeaderAndFooterPacket.reset(this.getProtocolVersion()));
    }
  }

  public void clearPlayerListHeaderAndFooterSilent() {
    this.playerListHeader = Component.empty();
    this.playerListFooter = Component.empty();
  }

  @Override
  public InternalTabList getTabList() {
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

    if (server.getConfiguration().isLogPlayerConnections()) {
      logger.info(Component.text(this + " has disconnected: ").append(translated));
    }
    connection.closeWith(DisconnectPacket.create(translated,
            this.getProtocolVersion(), connection.getState()));
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
    if (connectedServer != null && connectedServer.getServerInfo().equals(server.getServerInfo())) {
      friendlyError = Component.translatable("velocity.error.connected-server-error",
          Component.text(server.getServerInfo().getName()));
    } else {
      logger.error("{}: unable to connect to server {}", this, server.getServerInfo().getName(),
          wrapped);
      friendlyError = Component.translatable("velocity.error.connecting-server-error",
          Component.text(server.getServerInfo().getName()));
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
  public void handleConnectionException(RegisteredServer server, DisconnectPacket disconnect,
                                        boolean safe) {
    if (!isActive()) {
      // If the connection is no longer active, it makes no sense to try and recover it.
      return;
    }

    Component disconnectReason = disconnect.getReason().getComponent();
    String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
    if (connectedServer != null && connectedServer.getServerInfo().equals(server.getServerInfo())) {
      logger.info("{}: kicked from server {}: {}", this, server.getServerInfo().getName(),
          plainTextReason);
      handleConnectionException(server, disconnectReason,
          Component.translatable("velocity.error.moved-to-new-server", NamedTextColor.RED,
              Component.text(server.getServerInfo().getName()),
              disconnectReason), safe);
    } else {
      logger.error("{}: disconnected while connecting to {}: {}", this,
          server.getServerInfo().getName(), plainTextReason);
      handleConnectionException(server, disconnectReason,
          Component.translatable("velocity.error.cant-connect", NamedTextColor.RED,
              Component.text(server.getServerInfo().getName()),
              disconnectReason), safe);
    }
  }

  private void handleConnectionException(RegisteredServer rs,
                                         @Nullable Component kickReason, Component friendlyReason,
                                         boolean safe) {
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
      result =
          next.map(RedirectPlayer::create).orElseGet(() -> DisconnectPlayer.create(friendlyReason));
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
    server.getEventManager().fire(originalEvent).thenAcceptAsync(event -> {
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

      if (event.getResult() instanceof final DisconnectPlayer res) {
        disconnect(res.getReasonComponent());
      } else if (event.getResult() instanceof final RedirectPlayer res) {
        createConnectionRequest(res.getServer(), previousConnection).connect()
            .whenCompleteAsync((status, throwable) -> {
              if (throwable != null) {
                handleConnectionException(
                    status != null ? status.getAttemptedConnection() : res.getServer(), throwable,
                    true);
                return;
              }

              switch (status.getStatus()) {
                // Impossible/nonsensical cases
                case ALREADY_CONNECTED:
                  logger.error("{}: already connected to {}", this,
                      status.getAttemptedConnection().getServerInfo().getName());
                  break;
                case CONNECTION_IN_PROGRESS:
                  // Fatal case
                case CONNECTION_CANCELLED:
                  Component fallbackMsg = res.getMessageComponent();
                  if (fallbackMsg == null) {
                    fallbackMsg = friendlyReason;
                  }
                  disconnect(status.getReasonComponent().orElse(fallbackMsg));
                  break;
                case SERVER_DISCONNECTED:
                  Component reason = status.getReasonComponent()
                      .orElse(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
                  handleConnectionException(res.getServer(),
                      DisconnectPacket.create(reason, getProtocolVersion(), connection.getState()),
                      ((Impl) status).isSafe());
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
      } else if (event.getResult() instanceof final Notify res) {
        if (event.kickedDuringServerConnect() && previousConnection != null) {
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
   * @return the next server to try
   */
  private Optional<RegisteredServer> getNextServerToTry(@Nullable RegisteredServer current) {
    if (serversToTry == null) {
      String virtualHostStr = getVirtualHost().map(InetSocketAddress::getHostString)
          .orElse("")
          .toLowerCase(Locale.ROOT);
      serversToTry = server.getConfiguration().getForcedHosts().getOrDefault(virtualHostStr,
          Collections.emptyList());
    }

    if (serversToTry.isEmpty()) {
      List<String> connOrder = server.getConfiguration().getAttemptConnectionOrder();
      if (connOrder.isEmpty()) {
        return Optional.empty();
      } else {
        serversToTry = connOrder;
      }
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
      if (connectedPlayer.get().getCurrentServer().isEmpty()) {
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
    final boolean isPlayerAddressLoggingEnabled = server.getConfiguration()
        .isPlayerAddressLoggingEnabled();
    final String playerIp =
        isPlayerAddressLoggingEnabled ? getRemoteAddress().toString() : "<ip address withheld>";
    return "[connected player] " + profile.getName() + " (" + playerIp + ")";
  }

  @Override
  public Tristate getPermissionValue(String permission) {
    return permissionFunction.getPermissionValue(permission);
  }

  @Override
  public boolean sendPluginMessage(@NotNull ChannelIdentifier identifier, byte @NotNull [] data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");
    final PluginMessagePacket message = new PluginMessagePacket(identifier.getId(),
            Unpooled.wrappedBuffer(data));
    connection.write(message);
    return true;
  }

  @Override
  public boolean sendPluginMessage(
          final @NotNull ChannelIdentifier identifier,
          final @NotNull PluginMessageEncoder dataEncoder
  ) {
    requireNonNull(identifier);
    requireNonNull(dataEncoder);
    final ByteBuf buf = Unpooled.buffer();
    final ByteBufDataOutput dataOutput = new ByteBufDataOutput(buf);
    dataEncoder.encode(dataOutput);
    if (buf.isReadable()) {
      final PluginMessagePacket message = new PluginMessagePacket(identifier.getId(), buf);
      connection.write(message);
      return true;
    } else {
      buf.release();
      return false;
    }
  }

  @Override
  @Nullable
  public String getClientBrand() {
    return clientBrand;
  }

  void setClientBrand(final @Nullable String clientBrand) {
    this.clientBrand = clientBrand;
  }

  @Override
  public void transferToHost(final InetSocketAddress address) {
    Preconditions.checkNotNull(address);
    Preconditions.checkArgument(
            this.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_5) >= 0,
            "Player version must be 1.20.5 to be able to transfer to another host");

    server.getEventManager().fire(new PreTransferEvent(this, address)).thenAccept((event) -> {
      if (event.getResult().isAllowed()) {
        InetSocketAddress resultedAddress = event.getResult().address();
        if (resultedAddress == null) {
          resultedAddress = address;
        }
        connection.write(new TransferPacket(
                resultedAddress.getHostName(), resultedAddress.getPort()));
      }
    });
  }

  @Override
  public void storeCookie(final Key key, final byte[] data) {
    Preconditions.checkNotNull(key);
    Preconditions.checkNotNull(data);
    Preconditions.checkArgument(
        this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_5),
        "Player version must be at least 1.20.5 to be able to store cookies");

    if (connection.getState() != StateRegistry.PLAY
        && connection.getState() != StateRegistry.CONFIG) {
      throw new IllegalStateException("Can only store cookie in CONFIGURATION or PLAY protocol");
    }

    server.getEventManager().fire(new CookieStoreEvent(this, key, data))
        .thenAcceptAsync(event -> {
          if (event.getResult().isAllowed()) {
            final Key resultedKey = event.getResult().getKey() == null
                ? event.getOriginalKey() : event.getResult().getKey();
            final byte[] resultedData = event.getResult().getData() == null
                ? event.getOriginalData() : event.getResult().getData();

            connection.write(new ClientboundStoreCookiePacket(resultedKey, resultedData));
          }
        }, connection.eventLoop());
  }

  @Override
  public void requestCookie(final Key key) {
    Preconditions.checkNotNull(key);
    Preconditions.checkArgument(
        this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_5),
        "Player version must be at least 1.20.5 to be able to retrieve cookies");

    server.getEventManager().fire(new CookieRequestEvent(this, key))
        .thenAcceptAsync(event -> {
          if (event.getResult().isAllowed()) {
            final Key resultedKey = event.getResult().getKey() == null
                ? event.getOriginalKey() : event.getResult().getKey();

            connection.write(new ClientboundCookieRequestPacket(resultedKey));
          }
        }, connection.eventLoop());
  }

  @Override
  public void addCustomChatCompletions(@NotNull Collection<String> completions) {
    Preconditions.checkNotNull(completions, "completions");
    this.sendCustomChatCompletionPacket(completions, PlayerChatCompletionPacket.Action.ADD);
  }

  @Override
  public void removeCustomChatCompletions(@NotNull Collection<String> completions) {
    Preconditions.checkNotNull(completions, "completions");
    this.sendCustomChatCompletionPacket(completions, PlayerChatCompletionPacket.Action.REMOVE);
  }

  @Override
  public void setCustomChatCompletions(@NotNull Collection<String> completions) {
    Preconditions.checkNotNull(completions, "completions");
    this.sendCustomChatCompletionPacket(completions, PlayerChatCompletionPacket.Action.SET);
  }

  private void sendCustomChatCompletionPacket(@NotNull Collection<String> completions,
                                              PlayerChatCompletionPacket.Action action) {
    if (connection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19_1)) {
      connection.write(new PlayerChatCompletionPacket(completions.toArray(new String[0]), action));
    }
  }

  @Override
  public void spoofChatInput(String input) {
    Preconditions.checkArgument(input.length() <= LegacyChatPacket.MAX_SERVERBOUND_MESSAGE_LENGTH,
        "input cannot be greater than " + LegacyChatPacket.MAX_SERVERBOUND_MESSAGE_LENGTH
            + " characters in length");
    if (getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19)) {
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

  /**
   * Get the ResourcePackHandler corresponding to the player's version.
   *
   * @return the ResourcePackHandler of this player
   */
  public ResourcePackHandler resourcePackHandler() {
    return this.resourcePackHandler;
  }

  @Override
  @Deprecated
  public void sendResourcePack(String url) {
    sendResourcePackOffer(new VelocityResourcePackInfo.BuilderImpl(url).build());
  }

  @Override
  @Deprecated
  public void sendResourcePack(String url, byte[] hash) {
    sendResourcePackOffer(new VelocityResourcePackInfo.BuilderImpl(url).setHash(hash).build());
  }

  @Override
  public void sendResourcePackOffer(ResourcePackInfo packInfo) {
    this.resourcePackHandler.checkAlreadyAppliedPack(packInfo.getHash());
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      Preconditions.checkNotNull(packInfo, "packInfo");
      this.resourcePackHandler.queueResourcePack(packInfo);
    }
  }

  @Override
  public void sendResourcePacks(@NotNull ResourcePackRequest request) {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_8)) {
      Preconditions.checkNotNull(request, "packRequest");
      this.resourcePackHandler.queueResourcePack(request);
    }
  }

  @Override
  public void clearResourcePacks() {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      connection.write(new RemoveResourcePackPacket());
      this.resourcePackHandler.clearAppliedResourcePacks();
    }
  }

  @Override
  public void removeResourcePacks(@NotNull UUID id, @NotNull UUID @NotNull ... others) {
    if (this.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_3)) {
      Preconditions.checkNotNull(id, "packUUID");
      if (this.resourcePackHandler.remove(id)) {
        connection.write(new RemoveResourcePackPacket(id));
      }
      for (final UUID other : others) {
        if (this.resourcePackHandler.remove(other)) {
          connection.write(new RemoveResourcePackPacket(other));
        }
      }
    }
  }

  @Override
  public void removeResourcePacks(@NotNull ResourcePackRequest request) {
    for (final net.kyori.adventure.resource.ResourcePackInfo resourcePackInfo : request.packs()) {
      removeResourcePacks(resourcePackInfo.id());
    }
  }

  @Override
  public void removeResourcePacks(@NotNull ResourcePackRequestLike request) {
    removeResourcePacks(request.asResourcePackRequest());
  }

  @Override
  @SuppressWarnings("checkstyle:linelength")
  public void removeResourcePacks(@NotNull ResourcePackInfoLike request, @NotNull ResourcePackInfoLike @NotNull ... others) {
    removeResourcePacks(request.asResourcePackInfo().id());
    for (final ResourcePackInfoLike other : others) {
      removeResourcePacks(other.asResourcePackInfo().id());
    }
  }

  @Override
  @Deprecated
  public @Nullable ResourcePackInfo getAppliedResourcePack() {
    return this.resourcePackHandler.getFirstAppliedPack();
  }

  @Override
  @Deprecated
  public @Nullable ResourcePackInfo getPendingResourcePack() {
    return this.resourcePackHandler.getFirstPendingPack();
  }

  @Override
  public @NotNull Collection<ResourcePackInfo> getAppliedResourcePacks() {
    return this.resourcePackHandler.getAppliedResourcePacks();
  }

  @Override
  public @NotNull Collection<ResourcePackInfo> getPendingResourcePacks() {
    return this.resourcePackHandler.getPendingResourcePacks();
  }

  /**
   * Sends a {@link KeepAlivePacket} packet to the player with a random ID.
   * The response will be ignored by Velocity as it will not match
   * the ID last sent by the server.
   */
  public void sendKeepAlive() {
    if (connection.getState() == StateRegistry.PLAY
        || connection.getState() == StateRegistry.CONFIG) {
      KeepAlivePacket keepAlive = new KeepAlivePacket();
      keepAlive.setRandomId(ThreadLocalRandom.current().nextLong());
      connection.write(keepAlive);
    }
  }

  /**
   * Switches the connection to the client into config state.
   */
  public void switchToConfigState() {
    CompletableFuture.runAsync(() -> {
      connection.write(StartUpdatePacket.INSTANCE);
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
  public @Nullable IdentifiedKey getIdentifiedKey() {
    return playerKey;
  }

  private class IdentityImpl implements Identity {

    @Override
    public @NonNull UUID uuid() {
      return ConnectedPlayer.this.getUniqueId();
    }
  }

  @Override
  public ProtocolState getProtocolState() {
    return connection.getState().toProtocolState();
  }

  private final class ConnectionRequestBuilderImpl implements ConnectionRequestBuilder {

    private final RegisteredServer toConnect;
    private final @Nullable VelocityRegisteredServer previousServer;

    ConnectionRequestBuilderImpl(RegisteredServer toConnect,
                                 @Nullable VelocityServerConnection previousConnection) {
      this.toConnect = Preconditions.checkNotNull(toConnect, "info");
      this.previousServer = previousConnection == null ? null : previousConnection.getServer();
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
      if (connectedServer != null
          && connectedServer.getServer().getServerInfo().equals(server.getServerInfo())) {
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
        return server.getEventManager().fire(event).thenComposeAsync(newEvent -> {
          Optional<RegisteredServer> newDest = newEvent.getResult().getServer();
          if (newDest.isEmpty()) {
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
            handleConnectionException(status.getAttemptedConnection(), throwable, false);
          }
        }
      }, connection.eventLoop()).thenApply(x -> x);
    }

    @Override
    public CompletableFuture<Boolean> connectWithIndication() {
      return internalConnect().whenCompleteAsync((status, throwable) -> {
        if (throwable != null) {
          // TODO: The exception handling from this is not very good. Find a better way.
          handleConnectionException(status != null ? status.getAttemptedConnection() : toConnect,
              throwable, true);
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
            handleConnectionException(toConnect,
                DisconnectPacket.create(reason, getProtocolVersion(), connection.getState()), status.isSafe());
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
