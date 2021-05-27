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

package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.api.proxy.player.ConnectionRequestBuilder.Status.ALREADY_CONNECTED;
import static com.velocitypowered.proxy.connection.util.ConnectionRequestResults.plainResult;
import static com.velocitypowered.proxy.network.PluginMessageUtil.channelIdForVersion;
import static java.util.concurrent.CompletableFuture.completedFuture;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.velocitypowered.api.event.player.DisconnectEvent;
import com.velocitypowered.api.event.player.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.player.DisconnectEventImpl;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.event.player.KickedFromServerEvent.DisconnectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.Notify;
import com.velocitypowered.api.event.player.KickedFromServerEvent.RedirectPlayer;
import com.velocitypowered.api.event.player.KickedFromServerEvent.ServerKickResult;
import com.velocitypowered.api.event.player.KickedFromServerEventImpl;
import com.velocitypowered.api.event.player.PlayerClientSettingsChangedEventImpl;
import com.velocitypowered.api.event.player.PlayerModInfoEventImpl;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEventImpl;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.permission.PermissionProvider;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.connection.Player;
import com.velocitypowered.api.proxy.connection.ServerConnection;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.api.proxy.player.ClientSettings;
import com.velocitypowered.api.proxy.player.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftConnectionAssociation;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.network.PluginMessageUtil;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundChatPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundResourcePackRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTitlePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundChatPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundClientSettingsPacket;
import com.velocitypowered.proxy.network.registry.state.ProtocolStates;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import com.velocitypowered.proxy.tablist.VelocityTabList;
import com.velocitypowered.proxy.tablist.VelocityTabListLegacy;
import com.velocitypowered.proxy.util.collect.CappedSet;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import net.kyori.adventure.translation.GlobalTranslator;
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
  private @Nullable ClientSettings settings;
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
  public String username() {
    return profile.name();
  }

  @Override
  public UUID id() {
    return profile.uuid();
  }

  @Override
  public @Nullable ServerConnection connectedServer() {
    return connectedServer;
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
  public GameProfile gameProfile() {
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
  public boolean onlineMode() {
    return onlineMode;
  }

  @Override
  public ClientSettings clientSettings() {
    return settings == null ? ClientSettingsWrapper.DEFAULT : this.settings;
  }

  void setPlayerSettings(ServerboundClientSettingsPacket settings) {
    ClientSettingsWrapper cs = new ClientSettingsWrapper(settings);
    this.settings = cs;
    server.eventManager().fireAndForget(new PlayerClientSettingsChangedEventImpl(this, cs));
  }

  @Override
  public @Nullable ModInfo modInfo() {
    return modInfo;
  }

  public void setModInfo(ModInfo modInfo) {
    this.modInfo = modInfo;
    server.eventManager().fireAndForget(new PlayerModInfoEventImpl(this, modInfo));
  }

  @Override
  public @Nullable SocketAddress remoteAddress() {
    return connection.getRemoteAddress();
  }

  @Override
  public @Nullable InetSocketAddress connectedHostname() {
    return virtualHost;
  }

  void setPermissionFunction(PermissionFunction permissionFunction) {
    this.permissionFunction = permissionFunction;
  }

  @Override
  public boolean isActive() {
    return connection.getChannel().isActive();
  }

  @Override
  public ProtocolVersion protocolVersion() {
    return connection.getProtocolVersion();
  }

  public Component translateMessage(Component message) {
    Locale locale = this.settings == null ? Locale.getDefault() : this.settings.getLocale();
    return GlobalTranslator.render(message, locale);
  }

  @Override
  public void sendMessage(@NonNull Identity identity, @NonNull Component message,
      @NonNull MessageType type) {
    Preconditions.checkNotNull(message, "message");
    Preconditions.checkNotNull(type, "type");

    Component translated = translateMessage(message);

    connection.write(new ClientboundChatPacket(
        ProtocolUtils.getJsonChatSerializer(this.protocolVersion()).serialize(translated),
        type == MessageType.CHAT
            ? ClientboundChatPacket.CHAT_TYPE
            : ClientboundChatPacket.SYSTEM_TYPE,
        identity
    ));
  }

  @Override
  public void sendActionBar(net.kyori.adventure.text.@NonNull Component message) {
    Component translated = translateMessage(message);

    ProtocolVersion playerVersion = protocolVersion();
    if (playerVersion.gte(ProtocolVersion.MINECRAFT_1_11)) {
      // Use the title packet instead.
      connection.write(new ClientboundTitlePacket(
          ClientboundTitlePacket.SET_ACTION_BAR,
          ProtocolUtils.getJsonChatSerializer(playerVersion).serialize(translated)
      ));
    } else {
      // Due to issues with action bar packets, we'll need to convert the text message into a
      // legacy message and then inject the legacy text into a component... yuck!
      JsonObject object = new JsonObject();
      object.addProperty("text", LegacyComponentSerializer.legacySection()
          .serialize(translated));
      connection.write(new ClientboundChatPacket(
          object.toString(),
          ClientboundChatPacket.GAME_INFO_TYPE,
          Identity.nil()
      ));
    }
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
    this.tabList.setHeaderAndFooter(header, footer);
  }

  @Override
  public void showTitle(@NonNull Title title) {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      GsonComponentSerializer serializer = ProtocolUtils.getJsonChatSerializer(this
          .protocolVersion());

      connection.delayedWrite(new ClientboundTitlePacket(
          ClientboundTitlePacket.SET_TITLE,
          serializer.serialize(translateMessage(title.title()))
      ));

      connection.delayedWrite(new ClientboundTitlePacket(
          ClientboundTitlePacket.SET_SUBTITLE,
          serializer.serialize(translateMessage(title.subtitle()))
      ));

      Times times = title.times();
      if (times != null) {
        connection.delayedWrite(ClientboundTitlePacket.times(this.protocolVersion(), times));
      }

      connection.flush();
    }
  }

  @Override
  public void clearTitle() {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(ClientboundTitlePacket.hide(this.protocolVersion()));
    }
  }

  @Override
  public void resetTitle() {
    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(ClientboundTitlePacket.reset(this.protocolVersion()));
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
    return new ConnectionRequestBuilderImpl(server);
  }

  @Override
  public void setGameProfileProperties(List<GameProfile.Property> properties) {
    this.profile = profile.withProperties(properties);
  }

  @Override
  public VelocityTabList tabList() {
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
   * @param reason the reason for disconnecting the player
   * @param duringLogin whether the disconnect happened during login
   */
  public void disconnect0(Component reason, boolean duringLogin) {
    Component translated = this.translateMessage(reason);

    logger.info("{} has disconnected: {}", this,
        LegacyComponentSerializer.legacySection().serialize(translated));
    connection.closeWith(ClientboundDisconnectPacket.create(translated, this.protocolVersion()));
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
   * @param server the server we disconnected from
   * @param disconnect the disconnect packet
   * @param safe whether or not we can safely reconnect to a new server
   */
  public void handleConnectionException(RegisteredServer server, ClientboundDisconnectPacket disconnect,
      boolean safe) {
    if (!isActive()) {
      // If the connection is no longer active, it makes no sense to try and recover it.
      return;
    }

    Component disconnectReason = GsonComponentSerializer.gson().deserialize(disconnect.getReason());
    String plainTextReason = PASS_THRU_TRANSLATE.serialize(disconnectReason);
    if (connectedServer != null && connectedServer.serverInfo().equals(server.serverInfo())) {
      logger.error("{}: kicked from server {}: {}", this, server.serverInfo().name(),
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

    boolean kickedFromCurrent = connectedServer == null || connectedServer.target().equals(rs);
    ServerKickResult result;
    if (kickedFromCurrent) {
      RegisteredServer next = getNextServerToTry(rs);
      if (next == null) {
        result = DisconnectPlayer.create(friendlyReason);
      } else {
        result = RedirectPlayer.create(next);
      }
    } else {
      // If we were kicked by going to another server, the connection should not be in flight
      if (connectionInFlight != null && connectionInFlight.target().equals(rs)) {
        resetInFlightConnection();
      }
      result = Notify.create(friendlyReason);
    }
    KickedFromServerEvent originalEvent = new KickedFromServerEventImpl(this, rs, kickReason,
        !kickedFromCurrent, result);
    handleKickEvent(originalEvent, friendlyReason, kickedFromCurrent);
  }

  private void handleKickEvent(KickedFromServerEvent originalEvent, Component friendlyReason,
      boolean kickedFromCurrent) {
    server.eventManager().fire(originalEvent)
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

          if (event.result() instanceof DisconnectPlayer) {
            DisconnectPlayer res = (DisconnectPlayer) event.result();
            disconnect(res.message());
          } else if (event.result() instanceof RedirectPlayer) {
            RedirectPlayer res = (RedirectPlayer) event.result();
            createConnectionRequest(res.getServer())
                .connect()
                .whenCompleteAsync((status, throwable) -> {
                  if (throwable != null) {
                    handleConnectionException(status != null ? status.finalTarget()
                        : res.getServer(), throwable, true);
                    return;
                  }

                  switch (status.status()) {
                    // Impossible/nonsensical cases
                    case ALREADY_CONNECTED:
                    case CONNECTION_IN_PROGRESS:
                    // Fatal case
                    case CONNECTION_CANCELLED:
                      Component disconnectReason = status.failureReason();
                      if (disconnectReason == null) {
                        disconnectReason = res.message();
                        if (disconnectReason == null) {
                          disconnectReason = ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR;
                        }
                      }
                      disconnect(disconnectReason);
                      break;
                    case SERVER_DISCONNECTED:
                      Component reason = status.failureReason() != null ? status.failureReason()
                          : ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR;
                      handleConnectionException(res.getServer(), ClientboundDisconnectPacket.create(reason,
                          protocolVersion()), ((Impl) status).isSafe());
                      break;
                    case SUCCESS:
                      sendMessage(Component.translatable("velocity.error.moved-to-new-server",
                          NamedTextColor.RED,
                          Component.text(originalEvent.server().serverInfo().name()),
                          friendlyReason));
                      break;
                    default:
                      // The only remaining value is successful (no need to do anything!)
                      break;
                  }
                }, connection.eventLoop());
          } else if (event.result() instanceof Notify) {
            Notify res = (Notify) event.result();
            if (event.kickedDuringServerConnect() && previouslyConnected) {
              sendMessage(Identity.nil(), res.message());
            } else {
              disconnect(res.message());
            }
          } else {
            // In case someone gets creative, assume we want to disconnect the player.
            disconnect(friendlyReason);
          }
        }, connection.eventLoop())
        .exceptionally(throwable -> {
          logger.error("Unable to handle server disconnection for {}", this, throwable);
          disconnect(friendlyReason);
          return null;
        });
  }

  /**
   * Finds another server to attempt to log into, if we were unexpectedly disconnected from the
   * server.
   *
   * @return the next server to try
   */
  public @Nullable RegisteredServer getNextServerToTry() {
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
  private @Nullable RegisteredServer getNextServerToTry(@Nullable RegisteredServer current) {
    if (serversToTry == null) {
      InetSocketAddress vhost = connectedHostname();
      String virtualHostStr = vhost == null ? "" : vhost.getHostString().toLowerCase(Locale.ROOT);
      serversToTry = server.configuration().getForcedHosts().getOrDefault(virtualHostStr,
          Collections.emptyList());
    }

    if (serversToTry.isEmpty()) {
      serversToTry = server.configuration().getAttemptConnectionOrder();
    }

    for (int i = tryIndex; i < serversToTry.size(); i++) {
      String toTryName = serversToTry.get(i);
      if ((connectedServer != null && hasSameName(connectedServer.target(), toTryName))
          || (connectionInFlight != null && hasSameName(connectionInFlight.target(), toTryName))
          || (current != null && hasSameName(current, toTryName))) {
        continue;
      }

      tryIndex = i;
      return server.server(toTryName);
    }
    return null;
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

    Player connectedPlayer = server.player(this.id());
    server.unregisterConnection(this);

    LoginStatus status;
    if (connectedPlayer != null) {
      if (connectedPlayer.connectedServer() != null) {
        status = LoginStatus.PRE_SERVER_JOIN;
      } else {
        status = connectedPlayer == this ? LoginStatus.SUCCESSFUL_LOGIN
            : LoginStatus.CONFLICTING_LOGIN;
      }
    } else {
      status = connection.isKnownDisconnect() ? LoginStatus.CANCELLED_BY_PROXY :
          LoginStatus.CANCELLED_BY_USER;
    }

    DisconnectEvent event = new DisconnectEventImpl(this, status);
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
    return "[connected player] " + profile.name() + " (" + remoteAddress() + ")";
  }

  @Override
  public Tristate evaluatePermission(String permission) {
    return permissionFunction.evaluatePermission(permission);
  }

  @Override
  public boolean sendPluginMessage(PluginChannelId identifier, byte[] data) {
    Preconditions.checkNotNull(identifier, "identifier");
    Preconditions.checkNotNull(data, "data");
    ClientboundPluginMessagePacket message = new ClientboundPluginMessagePacket(
        channelIdForVersion(identifier, connection.getProtocolVersion()),
        Unpooled.wrappedBuffer(data));
    connection.write(message);
    return true;
  }

  @Override
  public void spoofChatInput(String input) {
    Preconditions.checkArgument(input.length() <= ServerboundChatPacket.MAX_MESSAGE_LENGTH,
        "input cannot be greater than " + ServerboundChatPacket.MAX_MESSAGE_LENGTH
            + " characters in length");
    ensureBackendConnection().write(new ServerboundChatPacket(input));
  }

  @Override
  public void sendResourcePack(String url) {
    Preconditions.checkNotNull(url, "url");

    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(new ClientboundResourcePackRequestPacket(url, ""));
    }
  }

  @Override
  public void sendResourcePack(String url, byte[] hash) {
    Preconditions.checkNotNull(url, "url");
    Preconditions.checkNotNull(hash, "hash");
    Preconditions.checkArgument(hash.length == 20, "Hash length is not 20");

    if (this.protocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      connection.write(new ClientboundResourcePackRequestPacket(url, ByteBufUtil.hexDump(hash)));
    }
  }

  /**
   * Sends a {@link ClientboundKeepAlivePacket} packet to the player with a random ID.
   * The response will be ignored by Velocity as it will not match the
   * ID last sent by the server.
   */
  public void sendKeepAlive() {
    if (connection.getState() == ProtocolStates.PLAY) {
      connection.write(new ClientboundKeepAlivePacket(ThreadLocalRandom.current().nextLong()));
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
  public boolean canForwardPluginMessage(ProtocolVersion version, AbstractPluginMessagePacket<?> message) {
    boolean minecraftOrFmlMessage;

    // By default, all internal Minecraft and Forge channels are forwarded from the server.
    if (version.lte(ProtocolVersion.MINECRAFT_1_12_2)) {
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
      return ConnectedPlayer.this.id();
    }
  }

  private class ConnectionRequestBuilderImpl implements ConnectionRequestBuilder {

    private final RegisteredServer toConnect;

    ConnectionRequestBuilderImpl(RegisteredServer toConnect) {
      this.toConnect = Preconditions.checkNotNull(toConnect, "info");
    }

    @Override
    public RegisteredServer target() {
      return toConnect;
    }

    private ConnectionRequestBuilder.@Nullable Status checkServer(RegisteredServer server) {
      Preconditions.checkArgument(server instanceof VelocityRegisteredServer,
          "Not a valid Velocity server.");
      if (connectionInFlight != null || (connectedServer != null
          && !connectedServer.hasCompletedJoin())) {
        return ConnectionRequestBuilder.Status.CONNECTION_IN_PROGRESS;
      }
      if (connectedServer != null && connectedServer.target().equals(server)) {
        return ALREADY_CONNECTED;
      }
      return null;
    }

    private CompletableFuture<ConnectionRequestBuilder.Status> getInitialStatus() {
      return CompletableFuture.supplyAsync(() -> checkServer(toConnect), connection.eventLoop());
    }

    private CompletableFuture<Impl> internalConnect() {
      return this.getInitialStatus()
          .thenCompose(initialCheck -> {
            if (initialCheck != null) {
              return completedFuture(plainResult(initialCheck, toConnect));
            }

            ServerPreConnectEvent event = new ServerPreConnectEventImpl(ConnectedPlayer.this,
                toConnect);
            return server.eventManager().fire(event)
                .thenComposeAsync(newEvent -> {
                  RegisteredServer realDestination = newEvent.result().target();
                  if (realDestination == null) {
                    return completedFuture(
                        plainResult(ConnectionRequestBuilder.Status.CONNECTION_CANCELLED, toConnect)
                    );
                  }

                  ConnectionRequestBuilder.Status secondCheck = checkServer(realDestination);
                  if (secondCheck != null) {
                    return completedFuture(plainResult(secondCheck, realDestination));
                  }

                  VelocityRegisteredServer vrs = (VelocityRegisteredServer) realDestination;
                  VelocityServerConnection con = new VelocityServerConnection(vrs,
                      ConnectedPlayer.this, server);
                  connectionInFlight = con;
                  return con.connect().thenApplyAsync((result) -> {
                    this.resetIfInFlightIs(con);
                    return result;
                  }, connection.eventLoop());
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
                handleConnectionException(status.finalTarget(), throwable, false);
                return;
              }
            }
            if (throwable != null) {
              logger.error("Exception during connect; status = {}", status, throwable);
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
              handleConnectionException(status != null ? status.finalTarget()
                  : toConnect, throwable, true);
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
                Component reason = status.failureReason() != null ? status.failureReason()
                    : ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR;
                handleConnectionException(toConnect, ClientboundDisconnectPacket.create(reason,
                    protocolVersion()), status.isSafe());
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
