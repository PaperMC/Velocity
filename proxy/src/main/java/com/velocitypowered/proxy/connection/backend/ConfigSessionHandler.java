/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerResourcePackSendEvent;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientConfigSessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySync;
import com.velocitypowered.proxy.protocol.packet.config.StartUpdate;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdate;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A special session handler that catches "last minute" disconnects. This version is to accommodate
 * 1.20.2+ switching. Yes, some of this is exceptionally stupid.
 */
public class ConfigSessionHandler implements MinecraftSessionHandler {

  private static final Pattern PLAUSIBLE_SHA1_HASH = Pattern.compile("^[a-z0-9]{40}$");
  private static final Logger logger = LogManager.getLogger(ConfigSessionHandler.class);
  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Impl> resultFuture;

  private ResourcePackInfo resourcePackToApply;

  private State state;

  /**
   * Creates the new transition handler.
   *
   * @param server       the Velocity server instance
   * @param serverConn   the server connection
   * @param resultFuture the result future
   */
  ConfigSessionHandler(VelocityServer server, VelocityServerConnection serverConn,
                       CompletableFuture<Impl> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;
    this.state = State.START;
  }

  @Override
  public void activated() {
    resourcePackToApply = serverConn.getPlayer().getAppliedResourcePack();
    serverConn.getPlayer().clearAppliedResourcePack();
  }

  @Override
  public boolean beforeHandle() {
    if (!serverConn.isActive()) {
      // Obsolete connection
      serverConn.disconnect();
      return true;
    }
    return false;
  }

  @Override
  public boolean handle(StartUpdate packet) {
    serverConn.ensureConnected().write(packet);
    return true;
  }

  @Override
  public boolean handle(TagsUpdate packet) {
    serverConn.getPlayer().getConnection().write(packet);
    return true;
  }

  @Override
  public boolean handle(KeepAlive packet) {
    serverConn.ensureConnected().write(packet);
    return true;
  }

  @Override
  public boolean handle(ResourcePackRequest packet) {
    final MinecraftConnection playerConnection = serverConn.getPlayer().getConnection();

    ServerResourcePackSendEvent event =
        new ServerResourcePackSendEvent(packet.toServerPromptedPack(), this.serverConn);

    server.getEventManager().fire(event).thenAcceptAsync(serverResourcePackSendEvent -> {
      if (playerConnection.isClosed()) {
        return;
      }
      if (serverResourcePackSendEvent.getResult().isAllowed()) {
        ResourcePackInfo toSend = serverResourcePackSendEvent.getProvidedResourcePack();
        if (toSend != serverResourcePackSendEvent.getReceivedResourcePack()) {
          ((VelocityResourcePackInfo) toSend).setOriginalOrigin(
              ResourcePackInfo.Origin.DOWNSTREAM_SERVER);
        }

        resourcePackToApply = null;
        serverConn.getPlayer().queueResourcePack(toSend);
      } else if (serverConn.getConnection() != null) {
        serverConn.getConnection().write(new ResourcePackResponse(packet.getHash(),
            PlayerResourcePackStatusEvent.Status.DECLINED));
      }
    }, playerConnection.eventLoop()).exceptionally((ex) -> {
      if (serverConn.getConnection() != null) {
        serverConn.getConnection().write(new ResourcePackResponse(packet.getHash(),
            PlayerResourcePackStatusEvent.Status.DECLINED));
      }
      logger.error("Exception while handling resource pack send for {}", playerConnection, ex);
      return null;
    });

    return true;
  }

  @Override
  public boolean handle(FinishedUpdate packet) {
    MinecraftConnection smc = serverConn.ensureConnected();
    ConnectedPlayer player = serverConn.getPlayer();
    ClientConfigSessionHandler configHandler =
        (ClientConfigSessionHandler) player.getConnection().getActiveSessionHandler();

    smc.setAutoReading(false);
    // Even when not auto reading messages are still decoded. Decode them with the correct state
    smc.getChannel().pipeline().get(MinecraftDecoder.class).setState(StateRegistry.PLAY);
    configHandler.handleBackendFinishUpdate(serverConn).thenAcceptAsync((unused) -> {
      if (serverConn == player.getConnectedServer()) {
        smc.setActiveSessionHandler(StateRegistry.PLAY);
        player.sendPlayerListHeaderAndFooter(
            player.getPlayerListHeader(), player.getPlayerListFooter());
        // The client cleared the tab list. TODO: Restore changes done via TabList API
        player.getTabList().clearAllSilent();
      } else {
        smc.setActiveSessionHandler(StateRegistry.PLAY,
            new TransitionSessionHandler(server, serverConn, resultFuture));
      }
      if (player.getAppliedResourcePack() == null && resourcePackToApply != null) {
        player.queueResourcePack(resourcePackToApply);
      }
      smc.setAutoReading(true);
    }, smc.eventLoop());
    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    serverConn.disconnect();
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    return true;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (PluginMessageUtil.isMcBrand(packet)) {
      serverConn.getPlayer().getConnection().write(
          PluginMessageUtil.rewriteMinecraftBrand(packet, server.getVersion(),
              serverConn.getPlayer().getProtocolVersion()));
    } else {
      // TODO: Change this so its usable for mod loaders
      serverConn.disconnect();
      resultFuture.complete(ConnectionRequestResults.forDisconnect(
          Component.translatable("multiplayer.disconnect.missing_tags"), serverConn.getServer()));
    }
    return true;
  }

  @Override
  public boolean handle(RegistrySync packet) {
    serverConn.getPlayer().getConnection().write(packet.retain());
    return true;
  }

  @Override
  public void disconnected() {
    resultFuture.completeExceptionally(
        new IOException("Unexpectedly disconnected from remote server"));
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    serverConn.getPlayer().getConnection().write(packet);
  }

  private void switchFailure(Throwable cause) {
    logger.error("Unable to switch to new server {} for {}", serverConn.getServerInfo().getName(),
        serverConn.getPlayer().getUsername(), cause);
    serverConn.getPlayer().disconnect(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
    resultFuture.completeExceptionally(cause);
  }

  /**
   * Represents the state of the configuration stage.
   */
  public static enum State {
    START, NEGOTIATING, PLUGIN_MESSAGE_INTERRUPT, RESOURCE_PACK_INTERRUPT, COMPLETE
  }
}
