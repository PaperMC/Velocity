package com.velocitypowered.proxy.connection.backend;

import static com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder.getBungeeCordChannel;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.PlayerAvailableCommandsEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.util.ConnectionMessages;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.timeout.ReadTimeoutException;
import java.util.Collection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BackendPlaySessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(BackendPlaySessionHandler.class);
  private static final boolean BACKPRESSURE_LOG = Boolean
      .getBoolean("velocity.log-server-backpressure");
  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final ClientPlaySessionHandler playerSessionHandler;
  private final MinecraftConnection playerConnection;
  private final BungeeCordMessageResponder bungeecordMessageResponder;
  private boolean exceptionTriggered = false;

  BackendPlaySessionHandler(VelocityServer server, VelocityServerConnection serverConn) {
    this.server = server;
    this.serverConn = serverConn;
    this.playerConnection = serverConn.getPlayer().getConnection();

    MinecraftSessionHandler psh = playerConnection.getSessionHandler();
    if (!(psh instanceof ClientPlaySessionHandler)) {
      throw new IllegalStateException(
          "Initializing BackendPlaySessionHandler with no backing client play session handler!");
    }
    this.playerSessionHandler = (ClientPlaySessionHandler) psh;

    this.bungeecordMessageResponder = new BungeeCordMessageResponder(server,
        serverConn.getPlayer());
  }

  @Override
  public void activated() {
    serverConn.getServer().addPlayer(serverConn.getPlayer());

    if (server.getConfiguration().isBungeePluginChannelEnabled()) {
      MinecraftConnection serverMc = serverConn.ensureConnected();
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
  public boolean handle(KeepAlive packet) {
    serverConn.getPendingPings().put(packet.getRandomId(), System.currentTimeMillis());
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
    if (bungeecordMessageResponder.process(packet)) {
      return true;
    }

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
      PluginMessage rewritten = PluginMessageUtil.rewriteMinecraftBrand(packet, server.getVersion(),
          playerConnection.getProtocolVersion());
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

    byte[] copy = ByteBufUtil.getBytes(packet.content());
    PluginMessageEvent event = new PluginMessageEvent(serverConn, serverConn.getPlayer(), id,
        copy);
    server.getEventManager().fire(event)
        .thenAcceptAsync(pme -> {
          if (pme.getResult().isAllowed() && !playerConnection.isClosed()) {
            PluginMessage copied = new PluginMessage(packet.getChannel(),
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
    RootCommandNode<CommandSource> rootNode = commands.getRootNode();
    if (server.getConfiguration().isAnnounceProxyCommands()) {
      // Inject commands from the proxy.
      RootCommandNode<CommandSource> dispatcherRootNode =
          (RootCommandNode<CommandSource>)
              filterNode(server.getCommandManager().getDispatcher().getRoot());
      assert dispatcherRootNode != null : "Filtering root node returned null.";
      Collection<CommandNode<CommandSource>> proxyNodes = dispatcherRootNode.getChildren();
      for (CommandNode<CommandSource> node : proxyNodes) {
        CommandNode<CommandSource> existingServerChild = rootNode.getChild(node.getName());
        if (existingServerChild != null) {
          rootNode.getChildren().remove(existingServerChild);
        }
        rootNode.addChild(node);
      }
    }

    server.getEventManager().fire(
        new PlayerAvailableCommandsEvent(serverConn.getPlayer(), rootNode))
        .thenAcceptAsync(event -> playerConnection.write(commands), playerConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while handling available commands for {}", playerConnection, ex);
          return null;
        });
    return true;
  }

  /**
   * Creates a deep copy of the provided command node, but removes any node that are not accessible
   * by the player (respecting the requirement of the node).
   *
   * @param source source node
   * @return filtered node
   */
  private CommandNode<CommandSource> filterNode(CommandNode<CommandSource> source) {
    CommandNode<CommandSource> dest;
    if (source instanceof RootCommandNode) {
      dest = new RootCommandNode<>();
    } else {
      if (source.getRequirement() != null) {
        try {
          if (!source.getRequirement().test(serverConn.getPlayer())) {
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

      dest = destChildBuilder.build();
    }

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
  public void handleGeneric(MinecraftPacket packet) {
    if (packet instanceof PluginMessage) {
      ((PluginMessage) packet).retain();
    }
    playerConnection.delayedWrite(packet);
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    playerConnection.delayedWrite(buf.retain());
  }

  @Override
  public void readCompleted() {
    playerConnection.flush();
  }

  @Override
  public void exception(Throwable throwable) {
    exceptionTriggered = true;
    serverConn.getPlayer().handleConnectionException(serverConn.getServer(), throwable,
        !(throwable instanceof ReadTimeoutException));
  }

  public VelocityServer getServer() {
    return server;
  }

  @Override
  public void disconnected() {
    serverConn.getServer().removePlayer(serverConn.getPlayer());
    if (!serverConn.isGracefulDisconnect() && !exceptionTriggered) {
      if (server.getConfiguration().isFailoverOnUnexpectedServerDisconnect()) {
        serverConn.getPlayer().handleConnectionException(serverConn.getServer(),
            Disconnect.create(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR,
                ProtocolVersion.MINECRAFT_1_16), true);
      } else {
        serverConn.getPlayer().disconnect(ConnectionMessages.INTERNAL_SERVER_CONNECTION_ERROR);
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
