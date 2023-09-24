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
import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.protocol.packet.config.ActiveFeatures;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySync;
import com.velocitypowered.proxy.protocol.packet.config.TagsUpdate;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import jdk.jfr.Experimental;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * A special session handler that catches "last minute" disconnects.
 * This version is to accommodate 1.20.2+ switching.
 * Yes, some of this is exceptionally stupid.
 */
@Experimental
public class ConfigSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(ConfigSessionHandler.class);

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Impl> resultFuture;


  private State state;

  /**
   * Creates the new transition handler.
   *
   * @param server       the Velocity server instance
   * @param serverConn   the server connection
   * @param resultFuture the result future
   */
  ConfigSessionHandler(VelocityServer server,
                       VelocityServerConnection serverConn,
                       CompletableFuture<Impl> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;
    this.state = State.START;
  }

  @Override
  public void deactivated() {
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
  public boolean handle(ClientSettings packet) {
    System.out.println("ClientSettings packet received");
    return true;
  }

  @Override
  public boolean handle(ActiveFeatures packet) {
    return true;
  }

  @Override
  public boolean handle(TagsUpdate packet) {
    return true;
  }

  @Override
  public boolean handle(KeepAlive packet) {
    System.out.println("KeepAlive packet received");
    serverConn.ensureConnected().write(packet);
    return true;
  }

  @Override
  public boolean handle(ResourcePackRequest packet) {
    System.out.println("ResourcePackRequest packet received");
    final MinecraftConnection playerConnection = serverConn.getPlayer().getConnection();

    ServerResourcePackSendEvent event = new ServerResourcePackSendEvent(
            packet.toServerPromptedPack(), this.serverConn);

    server.getEventManager().fire(event).thenAcceptAsync(serverResourcePackSendEvent -> {
      if (playerConnection.isClosed()) {
        return;
      }
      if (serverResourcePackSendEvent.getResult().isAllowed()) {
        ResourcePackInfo toSend = serverResourcePackSendEvent.getProvidedResourcePack();
        if (toSend != serverResourcePackSendEvent.getReceivedResourcePack()) {
          ((VelocityResourcePackInfo) toSend)
                  .setOriginalOrigin(ResourcePackInfo.Origin.DOWNSTREAM_SERVER);
        }

        serverConn.getPlayer().queueResourcePack(toSend);
      } else if (serverConn.getConnection() != null) {
        serverConn.getConnection().write(new ResourcePackResponse(
                packet.getHash(),
                PlayerResourcePackStatusEvent.Status.DECLINED
        ));
      }
    }, playerConnection.eventLoop()).exceptionally((ex) -> {
      if (serverConn.getConnection() != null) {
        serverConn.getConnection().write(new ResourcePackResponse(
                packet.getHash(),
                PlayerResourcePackStatusEvent.Status.DECLINED
        ));
      }
      logger.error("Exception while handling resource pack send for {}", playerConnection, ex);
      return null;
    });

    return true;
  }

  @Override
  public boolean handle(FinishedUpdate packet) {
    System.out.println("FinishedUpdate packet received");
    ClientConfigSessionHandler configHandler =
            (ClientConfigSessionHandler) serverConn.getPlayer().getConnection().getActiveSessionHandler();

    configHandler.handleBackendFinishUpdate(serverConn).thenAcceptAsync((unused) -> {
      serverConn.ensureConnected().write(new FinishedUpdate());
      serverConn.ensureConnected().setActiveSessionHandler(StateRegistry.PLAY,
              new TransitionSessionHandler(server, serverConn, resultFuture));
    }, serverConn.ensureConnected().eventLoop());
    return true;
  }


  private void switchFailure(Throwable cause) {
    logger.error("Unable to switch to new server {} for {}",
            serverConn.getServerInfo().getName(),
            serverConn.getPlayer().getUsername(), cause);
    serverConn.getPlayer().disconnect(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
    resultFuture.completeExceptionally(cause);
  }
  /*
    private ClientConfigSessionHandler getClientConfigSessionHandler(ConnectedPlayer player) {
      ClientPlaySessionHandler playHandler;
      if (player.getConnection().getSessionHandler() instanceof ClientPlaySessionHandler) {
        playHandler = (ClientPlaySessionHandler) player.getConnection().getSessionHandler();
      } else {
        playHandler = new ClientPlaySessionHandler(server, player);
      }

      ClientConfigSessionHandler configHandler;
      if (player.getConnection().getSessionHandler() instanceof ClientConfigSessionHandler) {
        configHandler = (ClientConfigSessionHandler) player.getConnection().getSessionHandler();
      } else {
        configHandler = new ClientConfigSessionHandler(server, player, playHandler);
      }
      return configHandler;
    }
  */
  @Override
  public boolean handle(Disconnect packet) {
    System.out.println("Disconnect packet received");
    serverConn.disconnect();
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    return true;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    System.out.println("PluginMessage packet received");
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
  public void disconnected() {
    resultFuture
            .completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
  }

  @Override
  public boolean handle(RegistrySync packet) {
    System.out.println("RegistrySync packet received");
    serverConn.getPlayer().getConnection().write(packet.retain());
    return true;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    System.out.println("MinecraftPacket packet received: " + packet.toString());
    serverConn.getPlayer().getConnection().write(packet);
  }

  public static enum State{
    START,
    NEGOTIATING,
    PLUGIN_MESSAGE_INTERRUPT,
    RESOURCE_PACK_INTERRUPT,
    COMPLETE
  }
}