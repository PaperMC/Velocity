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

package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder.getBungeeCordChannel;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.ServerResourcePackSendEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.command.CommandGraphInjector;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.network.pipeline.deser.MinecraftDecoder;
import com.velocitypowered.proxy.network.protocol.MinecraftPacket;
import com.velocitypowered.proxy.network.protocol.StateRegistry;
import com.velocitypowered.proxy.network.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.network.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.network.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.network.protocol.packet.Disconnect;
import com.velocitypowered.proxy.network.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.network.protocol.packet.LegacyPlayerListItem;
import com.velocitypowered.proxy.network.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.network.protocol.packet.RemovePlayerInfo;
import com.velocitypowered.proxy.network.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.network.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.network.protocol.packet.ServerData;
import com.velocitypowered.proxy.network.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.network.protocol.packet.UpsertPlayerInfo;
import com.velocitypowered.proxy.network.protocol.packet.config.StartUpdate;
import com.velocitypowered.proxy.network.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.timeout.ReadTimeoutException;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Handles a connected player.
 */
public class BackendPlaySessionHandler implements MinecraftSessionHandler {

  private static final Pattern PLAUSIBLE_SHA1_HASH = Pattern.compile("^[a-z0-9]{40}$");
  private static final Logger logger = LogManager.getLogger(BackendPlaySessionHandler.class);
  private static final boolean BACKPRESSURE_LOG =
      Boolean.getBoolean("velocity.log-server-backpressure");
  private static final int MAXIMUM_PACKETS_TO_FLUSH =
      Integer.getInteger("velocity.max-packets-per-flush", 8192);

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final ClientPlaySessionHandler playerSessionHandler;
  private final MinecraftConnection playerConnection;
  private final BungeeCordMessageResponder bungeecordMessageResponder;
  private boolean exceptionTriggered = false;
  private int packetsFlushed;

  BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
    this.server = server;
    this.serverConn = serverConn;
    this.playerConnection = serverConn.player().getConnection();

    MinecraftSessionHandler psh = playerConnection.getActiveSessionHandler();
    if (!(psh instanceof ClientPlaySessionHandler)) {
      throw new IllegalStateException(
          "Initializing BackendPlaySessionHandler with no backing client play session handler!");
    }
    this.playerSessionHandler = (ClientPlaySessionHandler) psh;

    this.bungeecordMessageResponder = new BungeeCordMessageResponder(server,
        serverConn.player());
  }

  @Override
  public void activated() {
    serverConn.server().addPlayer(serverConn.player());

    MinecraftConnection serverMc = serverConn.ensureConnected();
    if (server.configuration().isBungeePluginChannelEnabled()) {
      serverMc.write(PluginMessageUtil.constructChannelsPacket(serverMc.getProtocolVersion(),
          ImmutableList.of(getBungeeCordChannel(serverMc.getProtocolVersion()))
      ));
    }

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
    MinecraftConnection smc = serverConn.ensureConnected();
    smc.setAutoReading(false);
    // Even when not auto reading messages are still decoded. Decode them with the correct state
    smc.getChannel().pipeline().get(MinecraftDecoder.class).setState(StateRegistry.CONFIG);
    serverConn.player().switchToConfigState();
    return true;
  }

  @Override
  public boolean handle(KeepAlive packet) {
    serverConn.getPendingPings().put(packet.getRandomId(), System.currentTimeMillis());
    return false; // forwards on
  }

  @Override
  public boolean handle(ClientSettings packet) {
    serverConn.ensureConnected().write(packet);
    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    serverConn.disconnect();
    serverConn.player().handleConnectionException(serverConn.server(), packet, true);
    return true;
  }

  @Override
  public boolean handle(BossBarPacket packet) {
    if (packet.getAction() == BossBarPacket.ADD) {
      playerSessionHandler.getServerBossBars().add(packet.getUuid());
    } else if (packet.getAction() == BossBarPacket.REMOVE) {
      playerSessionHandler.getServerBossBars().remove(packet.getUuid());
    }
    return false; // forward
  }

  @Override
  public boolean handle(ResourcePackRequest packet) {
    ResourcePackInfo.Builder builder = new VelocityResourcePackInfo.BuilderImpl(
        Preconditions.checkNotNull(packet.getUrl()))
        .prompt(packet.getPrompt())
        .required(packet.isRequired())
        .setOrigin(ResourcePackInfo.Origin.DOWNSTREAM_SERVER);

    String hash = packet.getHash();
    if (hash != null && !hash.isEmpty()) {
      if (PLAUSIBLE_SHA1_HASH.matcher(hash).matches()) {
        builder.hash(ByteBufUtil.decodeHexDump(hash));
      }
    }

    ServerResourcePackSendEvent event = new ServerResourcePackSendEvent(
        builder.build(), this.serverConn);

    server.eventManager().fire(event).thenAcceptAsync(serverResourcePackSendEvent -> {
      if (playerConnection.isClosed()) {
        return;
      }
      if (serverResourcePackSendEvent.result().allowed()) {
        ResourcePackInfo toSend = serverResourcePackSendEvent.providedResourcePack();
        if (toSend != serverResourcePackSendEvent.receivedResourcePack()) {
          ((VelocityResourcePackInfo) toSend)
              .setOriginalOrigin(ResourcePackInfo.Origin.DOWNSTREAM_SERVER);
        }

        serverConn.player().queueResourcePack(toSend);
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
  public boolean handle(PluginMessage packet) {
    if (bungeecordMessageResponder.process(packet)) {
      return true;
    }

    // Register and unregister packets are simply forwarded to the server as-is.
    if (PluginMessageUtil.isRegister(packet) || PluginMessageUtil.isUnregister(packet)) {
      return false;
    }

    if (PluginMessageUtil.isMcBrand(packet)) {
      PluginMessage rewritten = PluginMessageUtil.rewriteMinecraftBrand(packet, server.version(),
          playerConnection.getProtocolVersion());
      playerConnection.write(rewritten);
      return true;
    }

    if (serverConn.getPhase().handle(serverConn, serverConn.player(), packet)) {
      // Handled.
      return true;
    }

    ChannelIdentifier id = server.channelRegistrar().getFromId(packet.getChannel());
    if (id == null) {
      return false;
    }

    byte[] copy = ByteBufUtil.getBytes(packet.content());
    PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.player(), id, copy);
    server.eventManager().fire(event).thenAcceptAsync(pme -> {
      if (pme.result().allowed() && !playerConnection.isClosed()) {
        PluginMessage copied = new PluginMessage(packet.getChannel(), Unpooled.wrappedBuffer(copy));
        playerConnection.write(copied);
      }
    }, playerConnection.eventLoop()).exceptionally((ex) -> {
      logger.error("Exception while handling plugin message {}", packet, ex);
      return null;
    });
    return true;
  }

  @Override
  public boolean handle(TabCompleteResponse packet) {
    playerSessionHandler.handleTabCompleteResponse(packet);
    return true;
  }

  @Override
  public boolean handle(LegacyPlayerListItem packet) {
    serverConn.player().tabList().processLegacy(packet);
    return false;
  }

  @Override
  public boolean handle(UpsertPlayerInfo packet) {
    serverConn.player().tabList().processUpdate(packet);
    return false;
  }

  @Override
  public boolean handle(RemovePlayerInfo packet) {
    serverConn.player().tabList().processRemove(packet);
    return false;
  }

  @Override
  public boolean handle(AvailableCommands commands) {
    RootCommandNode<CommandSource> rootNode = commands.getRootNode();
    if (server.configuration().isAnnounceProxyCommands()) {
      // Inject commands from the proxy.
      final CommandGraphInjector<CommandSource> injector = server.commandManager().getInjector();
      injector.inject(rootNode, serverConn.player());
    }

    server.eventManager().fire(
            new PlayerAvailableCommandsEvent(serverConn.player(), rootNode))
        .thenAcceptAsync(event -> playerConnection.write(commands), playerConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling available commands for {}", playerConnection, ex);
          return null;
        });
    return true;
  }

  @Override
  public boolean handle(ServerData packet) {
    server.getServerListPingHandler().getInitialPing(this.serverConn.player()).thenComposeAsync(
        ping -> server.eventManager()
            .fire(new ProxyPingEvent(this.serverConn.player(), ping)),
        playerConnection.eventLoop()).thenAcceptAsync(pingEvent -> this.playerConnection.write(
            new ServerData(pingEvent.ping().description(),
                pingEvent.ping().favicon().orElse(null), packet.isSecureChatEnforced())),
        playerConnection.eventLoop());
    return true;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof PluginMessage) {
      ((PluginMessage) packet).retain();
    }
    playerConnection.delayedWrite(packet);
    if (++packetsFlushed >= MAXIMUM_PACKETS_TO_FLUSH) {
      playerConnection.flush();
      packetsFlushed = 0;
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    playerConnection.delayedWrite(buf.retain());
    if (++packetsFlushed >= MAXIMUM_PACKETS_TO_FLUSH) {
      playerConnection.flush();
      packetsFlushed = 0;
    }
  }

  @Override
  public void readCompleted() {
    playerConnection.flush();
    packetsFlushed = 0;
  }

  @Override
  public void exception(Throwable throwable) {
    exceptionTriggered = true;
    serverConn.player().handleConnectionException(serverConn.server(), throwable,
        !(throwable instanceof ReadTimeoutException));
  }

  public VelocityServer getServer() {
    return server;
  }

  @Override
  public void disconnected() {
    serverConn.server().removePlayer(serverConn.player());
    if (!serverConn.isGracefulDisconnect() && !exceptionTriggered) {
      if (server.configuration().isFailoverOnUnexpectedServerDisconnect()) {
        serverConn.player().handleConnectionException(serverConn.server(),
            Disconnect.create(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR,
                ProtocolVersion.MINECRAFT_1_16), true);
      } else {
        serverConn.player().disconnect(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
      }
    }
  }

  @Override
  public void writabilityChanged() {
    Channel serverChan = serverConn.ensureConnected().getChannel();
    boolean writable = serverChan.isWritable();

    if (BACKPRESSURE_LOG) {
      if (writable) {
        logger.info("{} is not writable, not auto-reading player connection data", this.serverConn);
      } else {
        logger.info("{} is writable, will auto-read player connection data", this.serverConn);
      }
    }

    playerConnection.setAutoReading(writable);
  }
}
