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
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.PingIdentifyPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerboundCookieResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerboundCustomClickActionPacket;
import com.velocitypowered.proxy.protocol.packet.config.CodeOfConductAcceptPacket;
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
  private String brandChannel = null;

  private CompletableFuture<?> configurationFuture;
  private CompletableFuture<Void> configSwitchFuture;

  /**
   * Constructs a client config session handler.
   *
   * @param server the Velocity server instance
   * @param player the player
   */
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
  public boolean handle(final KeepAlivePacket packet) {
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
    return player.resourcePackHandler().onResourcePackResponse(
        new ResourcePackResponseBundle(packet.getId(),
            packet.getHash(),
            packet.getStatus())
    );
  }

  @Override
  public boolean handle(FinishedUpdatePacket packet) {
    player.getConnection().setActiveSessionHandler(StateRegistry.PLAY, new ClientPlaySessionHandler(server, player));

    configSwitchFuture.complete(null);
    return true;
  }

  @Override
  public boolean handle(final PluginMessagePacket packet) {
    final VelocityServerConnection serverConn = player.getConnectionInFlight();
    if (PluginMessageUtil.isMcBrand(packet)) {
      final String brand = PluginMessageUtil.readBrandMessage(packet.content());
      server.getEventManager().fireAndForget(new PlayerClientBrandEvent(player, brand));
      player.setClientBrand(brand);
      brandChannel = packet.getChannel();
      // Client sends `minecraft:brand` packet immediately after Login,
      // but at this time the backend server may not be ready
    } else if (BungeeCordMessageResponder.isBungeeCordMessage(packet)) {
      return true;
    } else if (serverConn != null) {
      byte[] bytes = ByteBufUtil.getBytes(packet.content());
      ChannelIdentifier id = this.server.getChannelRegistrar().getFromId(packet.getChannel());

      if (id == null) {
        serverConn.ensureConnected().write(packet.retain());
        return true;
      }

      // Handling this stuff async means that we should probably pause
      // the connection while we toss this off into another pool
      serverConn.getPlayer().getConnection().setAutoReading(false);
      this.server.getEventManager()
          .fire(new PluginMessageEvent(serverConn.getPlayer(), serverConn, id, bytes))
          .thenAcceptAsync(pme -> {
            if (pme.getResult().isAllowed() && serverConn.getConnection() != null) {
              serverConn.ensureConnected().write(new PluginMessagePacket(
                  pme.getIdentifier().getId(), Unpooled.wrappedBuffer(bytes)));
            }
            serverConn.getPlayer().getConnection().setAutoReading(true);
          }, player.getConnection().eventLoop()).exceptionally((ex) -> {
            logger.error("Exception while handling plugin message packet for {}", player, ex);
            return null;
          });
    }
    return true;
  }

  @Override
  public boolean handle(PingIdentifyPacket packet) {
    if (player.getConnectionInFlight() != null) {
      player.getConnectionInFlight().ensureConnected().write(packet);
      return true;
    }

    return false;
  }

  @Override
  public boolean handle(KnownPacksPacket packet) {
    callConfigurationEvent().thenRun(() -> {
      VelocityServerConnection targetServer =
          player.getConnectionInFlightOrConnectedServer();
      if (targetServer != null) {
        targetServer.ensureConnected().write(packet);
      }
    }).exceptionally(ex -> {
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
          if (event.getResult().isAllowed()) {
            final VelocityServerConnection serverConnection = player.getConnectionInFlight();
            if (serverConnection != null) {
              final Key resultedKey = event.getResult().getKey() == null
                  ? event.getOriginalKey() : event.getResult().getKey();
              final byte[] resultedData = event.getResult().getData() == null
                  ? event.getOriginalData() : event.getResult().getData();

              serverConnection.ensureConnected()
                  .write(new ServerboundCookieResponsePacket(resultedKey, resultedData));
            }
          }
        }, player.getConnection().eventLoop());

    return true;
  }

  @Override
  public boolean handle(ServerboundCustomClickActionPacket packet) {
    if (player.getConnectionInFlight() != null) {
      player.getConnectionInFlight().ensureConnected().write(packet.retain());
      return true;
    }

    return false;
  }

  @Override
  public boolean handle(CodeOfConductAcceptPacket packet) {
    if (this.player.getConnectionInFlight() != null) {
      this.player.getConnectionInFlight().ensureConnected().write(packet);
      return true;
    }

    return false;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      // No server connection yet, probably transitioning.
      return;
    }

    MinecraftConnection smc = serverConnection.getConnection();
    if (smc != null && serverConnection.getPhase().consideredComplete()) {
      if (packet instanceof PluginMessagePacket) {
        ((PluginMessagePacket) packet).retain();
      }
      smc.write(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    final VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      // No server connection yet, probably transitioning.
      return;
    }

    final MinecraftConnection smc = serverConnection.getConnection();
    if (smc != null && !smc.isClosed() && serverConnection.getPhase().consideredComplete()) {
      smc.write(buf.retain());
    }
  }

  @Override
  public void disconnected() {
    player.teardown();
  }

  @Override
  public void exception(Throwable throwable) {
    player.disconnect(Component.translatable("velocity.error.player-connection-error", NamedTextColor.RED));
  }

  /**
   * Calls the {@link PlayerConfigurationEvent}.
   * For 1.20.5+ backends this is done when the client responds to
   * the known packs request. The response is delayed until the event
   * has been called.
   * For 1.20.2-1.20.4 servers this is done when the client acknowledges
   * the end of the configuration.
   * This is handled differently because for 1.20.5+ servers can't keep
   * their connection alive between states and older servers don't have
   * the known packs transaction.
   */
  private CompletableFuture<?> callConfigurationEvent() {
    if (configurationFuture != null) {
      return configurationFuture;
    }
    return configurationFuture = server.getEventManager().fire(new PlayerConfigurationEvent(player, player.getConnectionInFlightOrConnectedServer()));
  }

  /**
   * Handles the backend finishing the config stage.
   *
   * @param serverConn the server connection
   * @return a future that completes when the config stage is finished
   */
  public CompletableFuture<Void> handleBackendFinishUpdate(VelocityServerConnection serverConn) {
    final MinecraftConnection smc = serverConn.ensureConnected();

    final String brand = serverConn.getPlayer().getClientBrand();
    if (brand != null && brandChannel != null) {
      final ByteBuf buf = Unpooled.buffer();
      ProtocolUtils.writeString(buf, brand);
      final PluginMessagePacket brandPacket = new PluginMessagePacket(brandChannel, buf);
      smc.write(brandPacket);
    }

    callConfigurationEvent().thenCompose(v -> {
      return server.getEventManager().fire(new PlayerFinishConfigurationEvent(player, serverConn))
          .completeOnTimeout(null, 5, TimeUnit.SECONDS);
    }).thenRunAsync(() -> {
      player.getConnection().write(FinishedUpdatePacket.INSTANCE);
      player.getConnection().getChannel().pipeline().get(MinecraftEncoder.class).setState(StateRegistry.PLAY);
      server.getEventManager().fireAndForget(new PlayerFinishedConfigurationEvent(player, serverConn));
    }, player.getConnection().eventLoop()).exceptionally(ex -> {
      logger.error("Error finishing configuration state:", ex);
      return null;
    });

    return configSwitchFuture;
  }
}
