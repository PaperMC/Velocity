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

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.CookieReceiveEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.event.player.configuration.PlayerConfigurationEvent;
import com.velocitypowered.api.event.player.configuration.PlayerFinishConfigurationEvent;
import com.velocitypowered.api.event.player.configuration.PlayerFinishedConfigurationEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.player.resourcepack.ResourcePackResponseBundle;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftEncoder;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdatePacket;
import com.velocitypowered.proxy.protocol.packet.config.KnownPacksPacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles the client config stage.
 */
public class ClientConfigSessionHandler implements MinecraftSessionHandler {
  private static final Logger logger = LogManager.getLogger(ClientConfigSessionHandler.class);

  private final VelocityServer server;
  private final ConnectedPlayer player;
  private String brandChannel;

  private CompletableFuture<?> configurationFuture;
  private CompletableFuture<Void> configSwitchFuture;

  public ClientConfigSessionHandler(VelocityServer server, ConnectedPlayer player) {
    this.server = server;
    this.player = player;
  }

  @Override
  public void activated() {
    configSwitchFuture = new CompletableFuture<>();
  }

  @Override
  public void deactivated() {
    configurationFuture = null;
  }

  @Override
  public boolean handle(KeepAlivePacket packet) {
    player.forwardKeepAlive(packet);
    return true;
  }

  @Override
  public boolean handle(ClientSettingsPacket packet) {
    player.setClientSettings(packet);
    return true;
  }

  @Override
  public boolean handle(ResourcePackResponsePacket packet) {
    return player.resourcePackHandler()
                 .onResourcePackResponse(new ResourcePackResponseBundle(
                     packet.getId(), packet.getHash(), packet.getStatus()));
  }

  @Override
  public boolean handle(FinishedUpdatePacket packet) {
    player.getConnection()
          .setActiveSessionHandler(StateRegistry.PLAY, new ClientPlaySessionHandler(server, player));
    configSwitchFuture.complete(null);
    return true;
  }

  @Override
  public boolean handle(PluginMessagePacket packet) {

    if (PluginMessageUtil.isMcBrand(packet)) {
      String brand = PluginMessageUtil.readBrandMessage(packet.content());
      server.getEventManager().fireAndForget(new PlayerClientBrandEvent(player, brand));
      player.setClientBrand(brand);
      brandChannel = packet.getChannel();
      return true;
    }

    if (BungeeCordMessageResponder.isBungeeCordMessage(packet)) {
      return true;
    }

    VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (serverConn != null) {
      ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
      byte[] data = ByteBufUtil.getBytes(packet.content());

      if (id == null) {
        serverConn.ensureConnected().write(packet.retain());
      } else {
        player.getConnection().setAutoReading(false);
        server.getEventManager()
              .fire(new PluginMessageEvent(player, serverConn, id, data))
              .thenAcceptAsync(event -> {
                if (event.getResult().isAllowed() && serverConn.getConnection() != null) {
                  serverConn.ensureConnected()
                            .write(new PluginMessagePacket(id.getId(), Unpooled.wrappedBuffer(data)));
                }
                player.getConnection().setAutoReading(true);
              }, player.getConnection().eventLoop())
              .exceptionally(ex -> {
                logger.error("Exception while handling plugin message packet for {}", player, ex);
                return null;
              });
      }
    }

    return true;
  }

  @Override
  public boolean handle(PingIdentifyPacket packet) {
    VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (serverConn != null) {
      serverConn.ensureConnected().write(packet);
      return true;
    }
    return false;
  }

  @Override
  public boolean handle(KnownPacksPacket packet) {
    callConfigurationEvent()
        .thenRun(() ->
            player.getConnectionInFlightOrConnectedServer()
                  .ensureConnected()
                  .write(packet))
        .exceptionally(ex -> {
          logger.error("Error forwarding known packs response to backend:", ex);
          return null;
        });
    return true;
  }

  @Override
  public boolean handle(ServerboundCookieResponsePacket packet) {
    server.getEventManager()
          .fire(new CookieReceiveEvent(player, packet.getKey(), packet.getPayload()))
          .thenAcceptAsync(event -> {
            if (!event.getResult().isAllowed()) {
              return;
            }
            VelocityServerConnection serverConn = player.getConnectionInFlight();
            if (serverConn != null) {
              Key key = event.getResult().getKey();
              byte[] data = event.getResult().getData();
              serverConn.ensureConnected()
                        .write(new ServerboundCookieResponsePacket(
                            key == null ? packet.getKey() : key,
                            data == null ? packet.getPayload() : data));
            }
          }, player.getConnection().eventLoop());
    return true;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn == null || !serverConn.getPhase().consideredComplete()) {
      return;
    }
    MinecraftConnection conn = serverConn.getConnection();
    if (conn != null) {
      if (packet instanceof PluginMessagePacket) {
        ((PluginMessagePacket) packet).retain();
      }
      conn.write(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn == null || serverConn.getConnection().isClosed() ||
        !serverConn.getPhase().consideredComplete()) {
      return;
    }
    serverConn.getConnection().write(buf.retain());
  }

  @Override
  public void disconnected() {
    player.teardown();
  }

  @Override
  public void exception(Throwable throwable) {
    player.disconnect(Component.translatable(
        "velocity.error.player-connection-error", NamedTextColor.RED));
  }

  private CompletableFuture<?> callConfigurationEvent() {
    if (configurationFuture == null) {
      configurationFuture = server.getEventManager()
                                  .fire(new PlayerConfigurationEvent(
                                      player, player.getConnectionInFlightOrConnectedServer()));
    }
    return configurationFuture;
  }

  public CompletableFuture<Void> handleBackendFinishUpdate(VelocityServerConnection serverConn) {
    MinecraftConnection conn = serverConn.ensureConnected();

    if (player.getClientBrand() != null && brandChannel != null) {
      ByteBuf buf = Unpooled.buffer();
      ProtocolUtils.writeString(buf, player.getClientBrand());
      conn.write(new PluginMessagePacket(brandChannel, buf));
    }

    callConfigurationEvent()
        .thenCompose(v ->
            server.getEventManager()
                  .fire(new PlayerFinishConfigurationEvent(player, serverConn))
                  .completeOnTimeout(null, 5, TimeUnit.SECONDS))
        .thenRunAsync(() -> {
          player.getConnection().write(FinishedUpdatePacket.INSTANCE);
          player.getConnection()
                .getChannel()
                .pipeline()
                .get(MinecraftEncoder.class)
                .setState(StateRegistry.PLAY);
          server.getEventManager()
                .fireAndForget(new PlayerFinishedConfigurationEvent(player, serverConn));
        }, player.getConnection().eventLoop())
        .exceptionally(ex -> {
          logger.error("Error finishing configuration state:", ex);
          return null;
        });

    return configSwitchFuture;
  }
}
