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

package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder.getBungeeCordChannel;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEventImpl;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PluginMessageEventImpl;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.network.java.PluginMessageUtil;
import com.velocitypowered.proxy.network.java.packet.AbstractPluginMessagePacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundAvailableCommandsPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundBossBarPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundKeepAlivePacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundPlayerListItemPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundPluginMessagePacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundTabCompleteResponsePacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.Packet;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.timeout.ReadTimeoutException;
import java.util.Collection;
import javax.annotation.Nullable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(BackendPlaySessionHandler.class);
  private static final boolean BACKPRESSURE_LOG = Boolean
      .getBoolean("velocity.log-server-backpressure");
  private static final int MAXIMUM_PACKETS_TO_FLUSH = Integer
      .getInteger("velocity.max-packets-per-flush", 8192);

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

    MinecraftSessionHandler psh = playerConnection.getSessionHandler();
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
    serverConn.target().addPlayer(serverConn.player());

    if (server.configuration().isBungeePluginChannelEnabled()) {
      MinecraftConnection serverMc = serverConn.ensureConnected();
      serverMc.write(PluginMessageUtil.constructChannelsPacket(serverMc.getProtocolVersion(),
          ImmutableList.of(getBungeeCordChannel(serverMc.getProtocolVersion())),
          ServerboundPluginMessagePacket.FACTORY));
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
  public boolean handle(ClientboundKeepAlivePacket packet) {
    serverConn.getPendingPings().put(packet.getRandomId(), System.currentTimeMillis());
    return false; // forwards on
  }

  @Override
  public boolean handle(ClientboundDisconnectPacket packet) {
    serverConn.disconnect();
    serverConn.player().handleConnectionException(serverConn.target(), packet, true);
    return true;
  }

  @Override
  public boolean handle(ClientboundBossBarPacket packet) {
    if (packet.getAction() == ClientboundBossBarPacket.ADD) {
      playerSessionHandler.getServerBossBars().add(packet.getUuid());
    } else if (packet.getAction() == ClientboundBossBarPacket.REMOVE) {
      playerSessionHandler.getServerBossBars().remove(packet.getUuid());
    }
    return false; // forward
  }

  @Override
  public boolean handle(ClientboundPluginMessagePacket packet) {
    if (bungeecordMessageResponder.process(packet)) {
      return true;
    }

    if (!serverConn.player().canForwardPluginMessage(serverConn.ensureConnected()
        .getProtocolVersion(), packet)) {
      return true;
    }

    // We need to specially handle REGISTER and UNREGISTER packets. Later on, we'll write them to
    // the client.
    if (PluginMessageUtil.isRegister(packet)) {
      serverConn.player().getKnownChannels().addAll(PluginMessageUtil.getChannels(packet));
      return false;
    } else if (PluginMessageUtil.isUnregister(packet)) {
      serverConn.player().getKnownChannels().removeAll(PluginMessageUtil.getChannels(packet));
      return false;
    }

    if (PluginMessageUtil.isMcBrand(packet)) {
      AbstractPluginMessagePacket<?> rewritten = PluginMessageUtil.rewriteMinecraftBrand(packet,
          server.version(), playerConnection.getProtocolVersion(), ClientboundPluginMessagePacket.FACTORY);
      playerConnection.write(rewritten);
      return true;
    }

    if (serverConn.getPhase().handle(serverConn, serverConn.player(), packet)) {
      // Handled.
      return true;
    }

    PluginChannelId id = server.channelRegistrar().getFromId(packet.getChannel());
    if (id == null) {
      return false;
    }

    byte[] copy = ByteBufUtil.getBytes(packet.content());
    PluginMessageEvent event = new PluginMessageEventImpl(serverConn, serverConn.player(), id,
        copy);
    server.eventManager().fire(event)
        .thenAcceptAsync(pme -> {
          if (pme.result().isAllowed() && !playerConnection.isClosed()) {
            ClientboundPluginMessagePacket copied = new ClientboundPluginMessagePacket(packet.getChannel(),
                Unpooled.wrappedBuffer(copy));
            playerConnection.write(copied);
          }
        }, playerConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling plugin message {}", packet, ex);
          return null;
        });
    return true;
  }

  @Override
  public boolean handle(ClientboundTabCompleteResponsePacket packet) {
    playerSessionHandler.handleTabCompleteResponse(packet);
    return true;
  }

  @Override
  public boolean handle(ClientboundPlayerListItemPacket packet) {
    serverConn.player().tabList().processBackendPacket(packet);
    return false; //Forward packet to player
  }

  @Override
  public boolean handle(ClientboundAvailableCommandsPacket commands) {
    RootCommandNode<CommandSource> rootNode = commands.getRootNode();
    if (server.configuration().isAnnounceProxyCommands()) {
      // Inject commands from the proxy.
      RootCommandNode<CommandSource> dispatcherRootNode = filterRootNode(server.commandManager()
          .getDispatcher().getRoot());
      Collection<CommandNode<CommandSource>> proxyNodes = dispatcherRootNode.getChildren();
      for (CommandNode<CommandSource> node : proxyNodes) {
        CommandNode<CommandSource> existingServerChild = rootNode.getChild(node.getName());
        if (existingServerChild != null) {
          rootNode.getChildren().remove(existingServerChild);
        }
        rootNode.addChild(node);
      }
    }

    server.eventManager().fire(
        new PlayerAvailableCommandsEventImpl(serverConn.player(), rootNode))
        .thenAcceptAsync(event -> playerConnection.write(commands), playerConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling available commands for {}", playerConnection, ex);
          return null;
        });
    return true;
  }

  /**
   * Creates a deep copy of the provided command node, but removes any node that are not accessible
   * by the player (respecting the requirement of the node). This function is specialized for
   * root command nodes, so as to get better safety guarantees.
   *
   * @param source source node
   * @return filtered node
   */
  private RootCommandNode<CommandSource> filterRootNode(CommandNode<CommandSource> source) {
    RootCommandNode<CommandSource> dest = new RootCommandNode<>();
    for (CommandNode<CommandSource> sourceChild : source.getChildren()) {
      CommandNode<CommandSource> destChild = filterNode(sourceChild);
      if (destChild == null) {
        continue;
      }
      dest.addChild(destChild);
    }

    return dest;
  }

  /**
   * Creates a deep copy of the provided command node, but removes any node that are not accessible
   * by the player (respecting the requirement of the node).
   *
   * @param source source node
   * @return filtered node
   */
  private @Nullable CommandNode<CommandSource> filterNode(CommandNode<CommandSource> source) {
    if (source.getRequirement() != null) {
      try {
        if (!source.getRequirement().test(serverConn.player())) {
          return null;
        }
      } catch (Throwable e) {
        // swallow everything because plugins
        logger.error(
            "Requirement test for command node " + source + " encountered an exception", e);
      }
    }

    ArgumentBuilder<CommandSource, ?> destChildBuilder = source.createBuilder();
    destChildBuilder.requires((commandSource) -> true);
    if (destChildBuilder.getRedirect() != null) {
      destChildBuilder.redirect(filterNode(destChildBuilder.getRedirect()));
    }

    CommandNode<CommandSource> dest = destChildBuilder.build();

    for (CommandNode<CommandSource> sourceChild : source.getChildren()) {
      CommandNode<CommandSource> destChild = filterNode(sourceChild);
      if (destChild == null) {
        continue;
      }
      dest.addChild(destChild);
    }

    return dest;
  }

  @Override
  public void handleGeneric(Packet packet) {
    if (packet instanceof AbstractPluginMessagePacket<?>) {
      ((AbstractPluginMessagePacket<?>) packet).retain();
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
    serverConn.player().handleConnectionException(serverConn.target(), throwable,
        !(throwable instanceof ReadTimeoutException));
  }

  public VelocityServer getServer() {
    return server;
  }

  @Override
  public void disconnected() {
    serverConn.target().removePlayer(serverConn.player());
    if (!serverConn.isGracefulDisconnect() && !exceptionTriggered) {
      if (server.configuration().isFailoverOnUnexpectedServerDisconnect()) {
        serverConn.player().handleConnectionException(serverConn.target(),
            ClientboundDisconnectPacket.create(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR,
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
