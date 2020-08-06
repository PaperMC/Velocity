package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.proxy.protocol.util.PluginMessageUtil.constructChannelsPacket;

import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse.Offer;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import io.netty.buffer.ByteBuf;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
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

  private final ConnectedPlayer player;
  private boolean spawned = false;
  private final List<UUID> serverBossBars = new ArrayList<>();
  private final Queue<PluginMessage> loginPluginMessages = new ArrayDeque<>();
  private final VelocityServer server;
  private @Nullable TabCompleteRequest legacyCommandTabComplete;

  /**
   * Constructs a client play session handler.
   * @param server the Velocity server instance
   * @param player the player
   */
  public ClientPlaySessionHandler(VelocityServer server, ConnectedPlayer player) {
    this.player = player;
    this.server = server;
  }

  @Override
  public void activated() {
    Collection<String> channels = server.getChannelRegistrar().getChannelsForProtocol(player
        .getProtocolVersion());
    if (!channels.isEmpty()) {
      PluginMessage register = constructChannelsPacket(player.getProtocolVersion(), channels);
      player.getMinecraftConnection().write(register);
      player.getKnownChannels().addAll(channels);
    }
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
        logger.info("Exception occurred while running command for {}", player.getUsername(),
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
    boolean isCommand = !packet.isAssumeCommand() && packet.getCommand().startsWith("/");

    if (!isCommand) {
      // We can't deal with anything else.
      return false;
    }

    // In 1.13+, we need to do additional work for the richer suggestions available.
    String command = packet.getCommand().substring(1);
    int spacePos = command.indexOf(' ');
    if (spacePos == -1) {
      return false;
    }

    String commandLabel = command.substring(0, spacePos);
    if (!server.getCommandManager().hasCommand(commandLabel)) {
      if (player.getProtocolVersion().compareTo(MINECRAFT_1_13) < 0) {
        // Outstanding tab completes are recorded for use with 1.12 clients and below to provide
        // tab list completion support for command names. In 1.13, Brigadier handles everything for
        // us.
        legacyCommandTabComplete = packet;
      }
      return false;
    }

    List<String> suggestions = server.getCommandManager().offerSuggestions(player, command);
    if (suggestions.isEmpty()) {
      return false;
    }

    List<Offer> offers = new ArrayList<>();
    for (String suggestion : suggestions) {
      offers.add(new Offer(suggestion));
    }

    int startPos = packet.getCommand().lastIndexOf(' ') + 1;
    if (startPos > 0) {
      TabCompleteResponse resp = new TabCompleteResponse();
      resp.setTransactionId(packet.getTransactionId());
      resp.setStart(startPos);
      resp.setLength(packet.getCommand().length() - startPos);
      resp.getOffers().addAll(offers);
      player.getMinecraftConnection().write(resp);
    }
    return true;
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    MinecraftConnection backendConn = serverConn != null ? serverConn.getConnection() : null;
    if (serverConn != null && backendConn != null) {
      if (backendConn.getState() != StateRegistry.PLAY) {
        logger.warn("A plugin message was received while the backend server was not "
            + "ready. Channel: {}. Packet discarded.", packet.getChannel());
      } else if (PluginMessageUtil.isRegister(packet)) {
        player.getKnownChannels().addAll(PluginMessageUtil.getChannels(packet));
        backendConn.write(packet);
      } else if (PluginMessageUtil.isUnregister(packet)) {
        player.getKnownChannels().removeAll(PluginMessageUtil.getChannels(packet));
        backendConn.write(packet);
      } else if (PluginMessageUtil.isMcBrand(packet)) {
        backendConn.write(PluginMessageUtil.rewriteMinecraftBrand(packet, server.getVersion()));
      } else {
        if (serverConn.getPhase() == BackendConnectionPhases.IN_TRANSITION) {
          // We must bypass the currently-connected server when forwarding Forge packets.
          VelocityServerConnection inFlight = player.getConnectionInFlight();
          if (inFlight != null) {
            player.getPhase().handle(player, packet, inFlight);
          }
          return true;
        }

        if (!player.getPhase().handle(player, packet, serverConn)) {
          if (!player.getPhase().consideredComplete() || !serverConn.getPhase()
              .consideredComplete()) {
            // The client is trying to send messages too early. This is primarily caused by mods,
            // but further aggravated by Velocity. To work around these issues, we will queue any
            // non-FML handshake messages to be sent once the FML handshake has completed or the
            // JoinGame packet has been received by the proxy, whichever comes first.
            loginPluginMessages.add(packet);
          } else {
            ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
            if (id == null) {
              backendConn.write(packet);
            } else {
              PluginMessageEvent event = new PluginMessageEvent(player, serverConn, id,
                  packet.getData());
              server.getEventManager().fire(event).thenAcceptAsync(pme -> {
                if (pme.getResult().isAllowed()) {
                  backendConn.write(packet);
                }
              }, backendConn.eventLoop());
            }
          }
        }
      }
    }

    return true;
  }

  @Override
  public boolean handle(ResourcePackResponse packet) {
    server.getEventManager().fireAndForget(new PlayerResourcePackStatusEvent(player,
        packet.getStatus()));
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
    player.disconnect(TextComponent.of("Your connection has encountered an error. Try again later.",
        TextColor.RED));
  }

  @Override
  public void writabilityChanged() {
    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn != null) {
      boolean writable = player.getMinecraftConnection().getChannel().isWritable();
      MinecraftConnection smc = serverConn.getConnection();
      if (smc != null) {
        smc.setAutoReading(writable);
      }
    }
  }

  /**
   * Handles the {@code JoinGame} packet. This function is responsible for handling the client-side
   * switching servers in Velocity.
   * @param joinGame the join game packet
   * @param destination the new server we are connecting to
   */
  public void handleBackendJoinGame(JoinGame joinGame, VelocityServerConnection destination) {
    final MinecraftConnection serverMc = destination.ensureConnected();

    if (!spawned) {
      // Nothing special to do with regards to spawning the player
      spawned = true;
      destination.setActiveDimensionRegistry(joinGame.getDimensionRegistry()); // 1.16
      player.getMinecraftConnection().delayedWrite(joinGame);
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
      player.getMinecraftConnection().delayedWrite(joinGame);
      // Since 1.16 this dynamic changed:
      // We don't need to send two dimension swiches anymore!
      if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
        int tempDim = joinGame.getDimension() == 0 ? -1 : 0;
        player.getMinecraftConnection().delayedWrite(
                new Respawn(tempDim, joinGame.getPartialHashedSeed(), joinGame.getDifficulty(),
                    joinGame.getGamemode(), joinGame.getLevelType(),
                    false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
                    joinGame.getCurrentDimensionData()));
      }

      player.getMinecraftConnection().delayedWrite(
          new Respawn(joinGame.getDimension(), joinGame.getPartialHashedSeed(),
              joinGame.getDifficulty(), joinGame.getGamemode(), joinGame.getLevelType(),
              false, joinGame.getDimensionInfo(), joinGame.getPreviousGamemode(),
              joinGame.getCurrentDimensionData()));
      destination.setActiveDimensionRegistry(joinGame.getDimensionRegistry()); // 1.16
    }

    // Remove previous boss bars. These don't get cleared when sending JoinGame, thus the need to
    // track them.
    for (UUID serverBossBar : serverBossBars) {
      BossBar deletePacket = new BossBar();
      deletePacket.setUuid(serverBossBar);
      deletePacket.setAction(BossBar.REMOVE);
      player.getMinecraftConnection().delayedWrite(deletePacket);
    }
    serverBossBars.clear();

    // Tell the server about this client's plugin message channels.
    ProtocolVersion serverVersion = serverMc.getProtocolVersion();
    if (!player.getKnownChannels().isEmpty()) {
      serverMc.delayedWrite(constructChannelsPacket(serverVersion, player.getKnownChannels()));
    }

    // If we had plugin messages queued during login/FML handshake, send them now.
    PluginMessage pm;
    while ((pm = loginPluginMessages.poll()) != null) {
      serverMc.delayedWrite(pm);
    }

    // Clear any title from the previous server.
    player.getMinecraftConnection()
        .delayedWrite(TitlePacket.resetForProtocolVersion(player.getProtocolVersion()));

    // Flush everything
    player.getMinecraftConnection().flush();
    serverMc.flush();
    destination.completeJoin();
  }

  public List<UUID> getServerBossBars() {
    return serverBossBars;
  }

  /**
   * Handles additional tab complete for 1.12 and lower clients.
   *
   * @param response the tab complete response from the backend
   */
  public void handleTabCompleteResponse(TabCompleteResponse response) {
    if (legacyCommandTabComplete != null) {
      String command = legacyCommandTabComplete.getCommand().substring(1);
      try {
        List<String> offers = server.getCommandManager().offerSuggestions(player, command);
        for (String offer : offers) {
          response.getOffers().add(new Offer(offer, null));
        }
        response.getOffers().sort(null);
      } catch (Exception e) {
        logger.error("Unable to provide tab list completions for {} for command '{}'",
            player.getUsername(),
            command, e);
      }
      legacyCommandTabComplete = null;
    }

    player.getMinecraftConnection().write(response);
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
