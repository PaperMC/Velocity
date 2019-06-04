package com.velocitypowered.proxy.connection.backend;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.server.ServerChatEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands.ProtocolSuggestionProvider;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.server.VelocityRegisteredServer;
import io.netty.buffer.ByteBuf;
import java.util.Optional;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final ClientPlaySessionHandler playerSessionHandler;
  private final MinecraftConnection playerConnection;
  private boolean exceptionTriggered = false;

  BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
    this.server = server;
    this.serverConn = serverConn;
    this.playerConnection = serverConn.getPlayer().getMinecraftConnection();

    MinecraftSessionHandler psh = playerConnection.getSessionHandler();
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
    serverConn.getPlayer().handleConnectionException(serverConn.getServer(), packet, true);
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
    if (!serverConn.getPlayer().canForwardPluginMessage(serverConn.ensureConnected()
        .getProtocolVersion(), packet)) {
      return true;
    }

    // We need to specially handle REGISTER and UNREGISTER packets. Later on, we'll write them to
    // the client.
    if (PluginMessageUtil.isRegister(packet)) {
      serverConn.getPlayer().getKnownChannels().addAll(PluginMessageUtil.getChannels(packet));
      return false;
    } else if (PluginMessageUtil.isUnregister(packet)) {
      serverConn.getPlayer().getKnownChannels().removeAll(PluginMessageUtil.getChannels(packet));
      return false;
    }

    if (PluginMessageUtil.isMcBrand(packet)) {
      PluginMessage rewritten = PluginMessageUtil.rewriteMinecraftBrand(packet,
          server.getVersion());
      playerConnection.write(rewritten);
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

    PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id,
        packet.getData());
    server.getEventManager().fire(event)
        .thenAcceptAsync(pme -> {
          if (pme.getResult().isAllowed() && !playerConnection.isClosed()) {
            playerConnection.write(packet);
          }
        }, playerConnection.eventLoop());
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
  public boolean handle(Chat chat) {
    VelocityRegisteredServer serverConnection = serverConn.getServer();
    if (serverConnection == null) {
      return true;
    }
    ServerChatEvent event = new ServerChatEvent(serverConnection, chat.getMessage());
    MinecraftConnection packetConnection = serverConn.getConnection();
    if (packetConnection == null) {
      return true;
    }
    server
        .getEventManager()
        .fire(event)
        .thenAcceptAsync(
            calledEvent -> {
              ServerChatEvent.ServerChatResult result = calledEvent.getResult();
              if (result.isAllowed()) {
                Optional<String> message = result.getMessage();
                if (message.isPresent()) {
                  String probablyNewMessage = message.get();
                  if (!probablyNewMessage.equalsIgnoreCase(chat.getMessage())) {
                    // only send a new packet when the message got changed
                    packetConnection.write(Chat.createServerbound(probablyNewMessage));
                  } else {
                    packetConnection.write(chat);
                  }
                } else {
                  packetConnection.write(chat);
                }
              }
            },
            packetConnection.eventLoop());
    return false;
  }

  @Override
  public void handleGeneric(MinecraftPacket packet) {
    playerConnection.write(packet);
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    playerConnection.write(buf.retain());
  }

  @Override
  public void exception(Throwable throwable) {
    exceptionTriggered = true;
    serverConn.getPlayer().handleConnectionException(serverConn.getServer(), throwable, true);
  }

  public VelocityServer getServer() {
    return server;
  }

  @Override
  public void disconnected() {
    serverConn.getServer().removePlayer(serverConn.getPlayer());
    if (!serverConn.isGracefulDisconnect() && !exceptionTriggered) {
      serverConn.getPlayer().disconnect(ConnectionMessages.UNEXPECTED_DISCONNECT);
    }
  }
}
