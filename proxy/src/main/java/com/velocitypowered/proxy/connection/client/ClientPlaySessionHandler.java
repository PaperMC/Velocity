package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.util.ThrowableUtils;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles communication with the connected Minecraft client. This is effectively the primary nerve
 * center that joins backend servers with players.
 */
public class ClientPlaySessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(ClientPlaySessionHandler.class);
  private static final int MAX_PLUGIN_CHANNELS = 1024;

  private final ConnectedPlayer player;
  private boolean spawned = false;
  private final List<UUID> serverBossBars = new ArrayList<>();
  private final Set<String> knownChannels = new HashSet<>();
  private final Queue<PluginMessage> loginPluginMessages = new ArrayDeque<>();
  private final VelocityServer server;
  private @Nullable TabCompleteRequest outstandingTabComplete;

  public ClientPlaySessionHandler(VelocityServer server, ConnectedPlayer player) {
    this.player = player;
    this.server = server;
  }

  @Override
  public void activated() {
    PluginMessage register = PluginMessageUtil.constructChannelsPacket(player.getProtocolVersion(),
        server.getChannelRegistrar().getChannelsForProtocol(player.getProtocolVersion()));
    player.getConnection().write(register);
  }

  @Override
  public boolean handle(KeepAlive packet) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection != null && packet.getRandomId() == serverConnection.getLastPingId()) {
      MinecraftConnection smc = serverConnection.getConnection();
      if (smc != null) {
        player.setPing(System.currentTimeMillis() - serverConnection.getLastPingSent());
        smc.write(packet);
        serverConnection.resetLastPingId();
      }
    }
    return true;
  }

  @Override
  public boolean handle(ClientSettings packet) {
    player.setPlayerSettings(packet);
    return false; // will forward onto the server
  }

  @Override
  public boolean handle(Chat packet) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      return true;
    }
    MinecraftConnection smc = serverConnection.getConnection();
    if (smc == null) {
      return true;
    }

    String msg = packet.getMessage();
    if (msg.startsWith("/")) {
      try {
        if (!server.getCommandManager().execute(player, msg.substring(1))) {
          return false;
        }
      } catch (Exception e) {
        logger
            .info("Exception occurred while running command for {}", player.getProfile().getName(),
                e);
        player.sendMessage(
            TextComponent.of("An error occurred while running this command.", TextColor.RED));
        return true;
      }
    } else {
      PlayerChatEvent event = new PlayerChatEvent(player, msg);
      server.getEventManager().fire(event)
          .thenAcceptAsync(pme -> {
            PlayerChatEvent.ChatResult chatResult = pme.getResult();
            if (chatResult.isAllowed()) {
              Optional<String> eventMsg = pme.getResult().getMessage();
              if (eventMsg.isPresent()) {
                smc.write(Chat.createServerbound(eventMsg.get()));
              } else {
                smc.write(packet);
              }
            }
          }, smc.eventLoop());
    }
    return true;
  }

  @Override
  public boolean handle(TabCompleteRequest packet) {
    // Record the request so that the outstanding request can be augmented later.
    if (!packet.isAssumeCommand() && packet.getCommand().startsWith("/")) {
      int spacePos = packet.getCommand().indexOf(' ');
      if (spacePos > 0) {
        String cmd = packet.getCommand().substring(1, spacePos);
        if (server.getCommandManager().hasCommand(cmd)) {
          List<String> suggestions = server.getCommandManager()
              .offerSuggestions(player, packet.getCommand().substring(1));
          if (!suggestions.isEmpty()) {
            TabCompleteResponse resp = new TabCompleteResponse();
            resp.getOffers().addAll(suggestions);
            player.getConnection().write(resp);
            return true;
          }
        }
      }
    }
    outstandingTabComplete = packet;
    return false;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    MinecraftConnection backendConn = serverConn != null ? serverConn.getConnection() : null;
    if (serverConn != null && backendConn != null) {
      if (backendConn.getState() != StateRegistry.PLAY) {
        logger.warn("Plugin message was sent while the backend was in PLAY. Channel: {}. Packet discarded.");
      } else if (PluginMessageUtil.isRegister(packet)) {
        List<String> actuallyRegistered = new ArrayList<>();
        List<String> channels = PluginMessageUtil.getChannels(packet);
        for (String channel : channels) {
          if (knownChannels.size() >= MAX_PLUGIN_CHANNELS && !knownChannels.contains(channel)) {
            throw new IllegalStateException("Too many plugin message channels registered");
          }
          if (knownChannels.add(channel)) {
            actuallyRegistered.add(channel);
          }
        }

        if (!actuallyRegistered.isEmpty()) {
          PluginMessage newRegisterPacket = PluginMessageUtil.constructChannelsPacket(backendConn
              .getProtocolVersion(), actuallyRegistered);
          backendConn.write(newRegisterPacket);
        }
      } else if (PluginMessageUtil.isUnregister(packet)) {
        List<String> channels = PluginMessageUtil.getChannels(packet);
        knownChannels.removeAll(channels);
        backendConn.write(packet);
      } else if (PluginMessageUtil.isMcBrand(packet)) {
        PluginMessage rewritten = PluginMessageUtil.rewriteMinecraftBrand(packet, server.getVersion());
        backendConn.write(rewritten);
      } else if (!player.getPhase().handle(player, this, packet)) {

        if (!player.getPhase().consideredComplete()
            || !serverConn.getPhase().consideredComplete()) {

            // The client is trying to send messages too early. This is primarily caused by mods, but
            // it's further aggravated by Velocity. To work around these issues, we will queue any
            // non-FML handshake messages to be sent once the FML handshake has completed or the JoinGame
            // packet has been received by the proxy, whichever comes first.
            loginPluginMessages.add(packet);
        } else {
          ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
          if (id == null) {
            backendConn.write(packet);
          } else {
            PluginMessageEvent event = new PluginMessageEvent(player, serverConn, id,
                packet.getData());
            server.getEventManager().fire(event).thenAcceptAsync(pme -> backendConn.write(packet),
                backendConn.eventLoop());
          }
        }
      }
    }

    return true;
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
      smc.write(packet);
    }
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      // No server connection yet, probably transitioning.
      return;
    }

    MinecraftConnection smc = serverConnection.getConnection();
    if (smc != null && serverConnection.getPhase().consideredComplete()) {
      smc.write(buf.retain());
    }
  }

  @Override
  public void disconnected() {
    player.teardown();
  }

  @Override
  public void exception(Throwable throwable) {
    player.disconnect(TextComponent.builder()
        .content("An exception occurred in your connection: ")
        .color(TextColor.RED)
        .append(TextComponent.of(ThrowableUtils.briefDescription(throwable), TextColor.WHITE))
        .build());
  }

  @Override
  public void writabilityChanged() {
    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn != null) {
      boolean writable = player.getConnection().getChannel().isWritable();
      MinecraftConnection smc = serverConn.getConnection();
      if (smc != null) {
        smc.getChannel().config().setAutoRead(writable);
      }
    }
  }

  public void handleBackendJoinGame(JoinGame joinGame) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn == null) {
      throw new IllegalStateException(
          "No server connection for " + player + ", but JoinGame packet received");
    }
    MinecraftConnection serverMc = serverConn.getConnection();
    if (serverMc == null) {
      throw new IllegalStateException(
          "Server connection for " + player + " is disconnected, but JoinGame packet received");
    }

    if (!spawned) {
      // Nothing special to do with regards to spawning the player
      spawned = true;
      player.getConnection().delayedWrite(joinGame);

      // Required for Legacy Forge
      player.getPhase().onFirstJoin(player);
    } else {
      // Clear tab list to avoid duplicate entries
      player.getTabList().clearAll();

      // In order to handle switching to another server, you will need to send three packets:
      //
      // - The join game packet from the backend server
      // - A respawn packet with a different dimension
      // - Another respawn with the correct dimension
      //
      // The two respawns with different dimensions are required, otherwise the client gets
      // confused.
      //
      // Most notably, by having the client accept the join game packet, we can work around the need
      // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
      // improving compatibility with mods.
      player.getConnection().delayedWrite(joinGame);
      int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
      player.getConnection().delayedWrite(
          new Respawn(tempDim, joinGame.getDifficulty(), joinGame.getGamemode(),
              joinGame.getLevelType()));
      player.getConnection().delayedWrite(
          new Respawn(joinGame.getDimension(), joinGame.getDifficulty(), joinGame.getGamemode(),
              joinGame.getLevelType()));
    }

    // Remove previous boss bars. These don't get cleared when sending JoinGame, thus the need to
    // track them.
    for (UUID serverBossBar : serverBossBars) {
      BossBar deletePacket = new BossBar();
      deletePacket.setUuid(serverBossBar);
      deletePacket.setAction(BossBar.REMOVE);
      player.getConnection().delayedWrite(deletePacket);
    }
    serverBossBars.clear();

    // Tell the server about this client's plugin message channels.
    ProtocolVersion serverVersion = serverMc.getProtocolVersion();
    Collection<String> toRegister = new HashSet<>(knownChannels);
    if (serverVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      toRegister.addAll(server.getChannelRegistrar().getModernChannelIds());
    } else {
      toRegister.addAll(server.getChannelRegistrar().getIdsForLegacyConnections());
    }
    if (!toRegister.isEmpty()) {
      serverMc.delayedWrite(PluginMessageUtil.constructChannelsPacket(serverVersion, toRegister));
    }

    // If we had plugin messages queued during login/FML handshake, send them now.
    PluginMessage pm;
    while ((pm = loginPluginMessages.poll()) != null) {
      serverMc.delayedWrite(pm);
    }

    // Clear any title from the previous server.
    player.getConnection()
        .delayedWrite(TitlePacket.resetForProtocolVersion(player.getProtocolVersion()));

    // Flush everything
    player.getConnection().flush();
    serverMc.flush();
    serverConn.completeJoin();
  }

  public List<UUID> getServerBossBars() {
    return serverBossBars;
  }

  public Set<String> getKnownChannels() {
    return knownChannels;
  }

  public void handleTabCompleteResponse(TabCompleteResponse response) {
    if (outstandingTabComplete != null) {
      if (!outstandingTabComplete.isAssumeCommand()) {
        String command = outstandingTabComplete.getCommand().substring(1);
        try {
          response.getOffers().addAll(server.getCommandManager().offerSuggestions(player, command));
        } catch (Exception e) {
          logger.error("Unable to provide tab list completions for {} for command '{}'",
              player.getUsername(),
              command, e);
        }
        outstandingTabComplete = null;
      }
      player.getConnection().write(response);
    }
  }

  /**
   * Immediately send any queued messages to the server.
   */
  public void flushQueuedMessages() {
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection != null) {
      MinecraftConnection connection = serverConnection.getConnection();
      if (connection != null) {
        PluginMessage pm;
        while ((pm = loginPluginMessages.poll()) != null) {
          connection.write(pm);
        }
      }
    }
  }
}
