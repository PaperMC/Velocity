package com.velocitypowered.proxy.connection.backend;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands.ProtocolSuggestionProvider;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final ClientPlaySessionHandler playerSessionHandler;

  BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
    this.server = server;
    this.serverConn = serverConn;

    MinecraftSessionHandler psh = serverConn.getPlayer().getMinecraftConnection()
        .getSessionHandler();
    if (!(psh instanceof ClientPlaySessionHandler)) {
      throw new IllegalStateException(
          "Initializing BackendPlaySessionHandler with no backing client play session handler!");
    }
    this.playerSessionHandler = (ClientPlaySessionHandler) psh;
  }

  @Override
  public void activated() {
    serverConn.getServer().addPlayer(serverConn.getPlayer());
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
  public boolean handle(KeepAlive packet) {
    serverConn.setLastPingId(packet.getRandomId());
    return false; // forwards on
  }

  @Override
  public boolean handle(Disconnect packet) {
    serverConn.disconnect();
    serverConn.getPlayer().handleConnectionException(serverConn.getServer(), packet);
    return true;
  }

  @Override
  public boolean handle(JoinGame packet) {
    playerSessionHandler.handleBackendJoinGame(packet);
    return true;
  }

  @Override
  public boolean handle(BossBar packet) {
    if (packet.getAction() == BossBar.ADD) {
      playerSessionHandler.getServerBossBars().add(packet.getUuid());
    } else if (packet.getAction() == BossBar.REMOVE) {
      playerSessionHandler.getServerBossBars().remove(packet.getUuid());
    }
    return false; // forward
  }

  @Override
  public boolean handle(PluginMessage packet) {
    if (!canForwardPluginMessage(packet)) {
      return true;
    }

    if (PluginMessageUtil.isMcBrand(packet)) {
      PluginMessage rewritten = PluginMessageUtil.rewriteMinecraftBrand(packet,
          server.getVersion());
      serverConn.getPlayer().getMinecraftConnection().write(rewritten);
      return true;
    }

    if (serverConn.getPhase().handle(serverConn, serverConn.getPlayer(), packet)) {
      // Handled.
      return true;
    }

    ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
    if (id == null) {
      return false;
    }

    MinecraftConnection clientConn = serverConn.getPlayer().getMinecraftConnection();
    PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id,
        packet.getData());
    server.getEventManager().fire(event)
        .thenAcceptAsync(pme -> {
          if (pme.getResult().isAllowed() && !clientConn.isClosed()) {
            clientConn.write(packet);
          }
        }, clientConn.eventLoop());
    return true;
  }

  @Override
  public boolean handle(TabCompleteResponse packet) {
    playerSessionHandler.handleTabCompleteResponse(packet);
    return true;
  }

  @Override
  public boolean handle(PlayerListItem packet) {
    serverConn.getPlayer().getTabList().processBackendPacket(packet);
    return false; //Forward packet to player
  }

  @Override
  public boolean handle(AvailableCommands commands) {
    // Inject commands from the proxy.
    for (String command : server.getCommandManager().getAllRegisteredCommands()) {
      if (!server.getCommandManager().hasPermission(serverConn.getPlayer(), command)) {
        continue;
      }

      LiteralCommandNode<Object> root = LiteralArgumentBuilder.literal(command)
          .then(RequiredArgumentBuilder.argument("args", StringArgumentType.greedyString())
              .suggests(new ProtocolSuggestionProvider("minecraft:ask_server"))
              .build())
          .executes((ctx) -> 0)
          .build();
      commands.getRootNode().addChild(root);
    }
    return false;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    serverConn.getPlayer().getMinecraftConnection().write(packet);
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    serverConn.getPlayer().getMinecraftConnection().write(buf.retain());
  }

  @Override
  public void exception(Throwable throwable) {
    serverConn.getPlayer().handleConnectionException(serverConn.getServer(), throwable);
  }

  public VelocityServer getServer() {
    return server;
  }

  @Override
  public void disconnected() {
    serverConn.getServer().removePlayer(serverConn.getPlayer());
    if (!serverConn.isGracefulDisconnect()) {
      serverConn.getPlayer().disconnect(ConnectionMessages.UNEXPECTED_DISCONNECT);
    }
  }

  private boolean canForwardPluginMessage(PluginMessage message) {
    MinecraftConnection mc = serverConn.getConnection();
    if (mc == null) {
      return false;
    }
    boolean minecraftOrFmlMessage;
    if (mc.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_12_2) <= 0) {
      String channel = message.getChannel();
      minecraftOrFmlMessage = channel.startsWith("MC|") || channel
          .startsWith(LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL);
    } else {
      minecraftOrFmlMessage = message.getChannel().startsWith("minecraft:");
    }
    return minecraftOrFmlMessage
        || playerSessionHandler.getKnownChannels().contains(message.getChannel())
        || server.getChannelRegistrar().registered(message.getChannel());
  }
}
