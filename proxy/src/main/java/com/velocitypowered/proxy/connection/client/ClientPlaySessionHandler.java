package com.velocitypowered.proxy.connection.client;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.protocol.util.PluginMessageUtil.constructChannelsPacket;

import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
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
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
  private @Nullable TabCompleteRequest outstandingTabComplete;

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
      player.getConnection().write(register);
      player.getKnownChannels().addAll(channels);
    }
  }

  @Override
  public void deactivated() {
    for (PluginMessage message : loginPluginMessages) {
      ReferenceCountUtil.release(message);
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
      String originalCommand = msg.substring(1);
      server.getCommandManager().callCommandEvent(player, msg.substring(1))
          .thenComposeAsync(event -> processCommandExecuteResult(originalCommand,
              event.getResult()))
          .exceptionally(e -> {
            logger.info("Exception occurred while running command for {}",
                player.getUsername(), e);
            player.sendMessage(
                TextComponent.of("An error occurred while running this command.",
                    TextColor.RED));
            return null;
          });
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

    if (isCommand) {
      return this.handleCommandTabComplete(packet);
    } else {
      return this.handleRegularTabComplete(packet);
    }
  }

  @Override
  public boolean handle(PluginMessage packet) {
    VelocityServerConnection serverConn = player.getConnectedServer();
    MinecraftConnection backendConn = serverConn != null ? serverConn.getConnection() : null;
    if (serverConn == null || backendConn == null) {
      return true;
    }

    if (backendConn.getState() != StateRegistry.PLAY) {
      logger.warn("A plugin message was received while the backend server was not "
          + "ready. Channel: {}. Packet discarded.", packet.getChannel());
      return true;
    }

    if (this.tryHandleVanillaPluginMessageChannel(packet, backendConn)) {
      return true;
    }

    if (serverConn.getPhase() == BackendConnectionPhases.IN_TRANSITION) {
      // We must bypass the currently-connected server when forwarding Forge packets.
      VelocityServerConnection inFlight = player.getConnectionInFlight();
      if (inFlight != null) {
        if (player.getPhase().handle(player, packet, inFlight)) {
          return true;
        }
      }
    } else if (!this.tryHandleForgeMessage(packet, serverConn)) {
      return true;
    }

    ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
    if (id == null) {
      backendConn.write(packet.retain());
    } else {
      byte[] copy = ByteBufUtil.getBytes(packet.content());
      PluginMessageEvent event = new PluginMessageEvent(player, serverConn, id,
          ByteBufUtil.getBytes(packet.content()));
      server.getEventManager().fire(event).thenAcceptAsync(pme -> {
        PluginMessage message = new PluginMessage(packet.getChannel(),
            Unpooled.wrappedBuffer(copy));
        backendConn.write(message);
      }, backendConn.eventLoop());
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
      if (packet instanceof PluginMessage) {
        ((PluginMessage) packet).retain();
      }
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
    boolean writable = player.getConnection().getChannel().isWritable();

    if (!writable) {
      // We might have packets queued for the server, so flush them now to free up memory.
      player.getConnection().flush();
    }

    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn != null) {
      MinecraftConnection smc = serverConn.getConnection();
      if (smc != null) {
        smc.setAutoReading(writable);
      }
    }
  }

  private boolean tryHandleVanillaPluginMessageChannel(PluginMessage packet,
      MinecraftConnection backendConn) {
    if (PluginMessageUtil.isRegister(packet)) {
      player.getKnownChannels().addAll(PluginMessageUtil.getChannels(packet));
      backendConn.write(packet.retain());
      return true;
    } else if (PluginMessageUtil.isUnregister(packet)) {
      player.getKnownChannels().removeAll(PluginMessageUtil.getChannels(packet));
      backendConn.write(packet.retain());
      return true;
    } else if (PluginMessageUtil.isMcBrand(packet)) {
      backendConn.write(PluginMessageUtil.rewriteMinecraftBrand(packet, server.getVersion(),
          player.getProtocolVersion()));
      return true;
    }
    return false;
  }

  private boolean tryHandleForgeMessage(PluginMessage packet, VelocityServerConnection serverConn) {
    if (player.getPhase().handle(player, packet, serverConn)) {
      return true;
    }

    if (!player.getPhase().consideredComplete() || !serverConn.getPhase().consideredComplete()) {
      // The client is trying to send messages too early. This is primarily caused by mods,
      // but further aggravated by Velocity. To work around these issues, we will queue any
      // non-FML handshake messages to be sent once the FML handshake has completed or the
      // JoinGame packet has been received by the proxy, whichever comes first.
      //
      // We also need to make sure to retain these packets so they can be flushed
      // appropriately.
      loginPluginMessages.add(packet.retain());
      return true;
    } else {
      return false;
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
      player.getConnection().delayedWrite(joinGame);

      // Required for Legacy Forge
      player.getPhase().onFirstJoin(player);
    } else {
      // Clear tab list to avoid duplicate entries
      player.getTabList().clearAll();

      // In order to handle switching to another server, you will need to send two packets:
      //
      // - The join game packet from the backend server, with a different dimension
      // - A respawn with the correct dimension
      //
      // Most notably, by having the client accept the join game packet, we can work around the need
      // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
      // improving compatibility with mods.
      int realDim = joinGame.getDimension();
      joinGame.setDimension(getFakeTemporaryDimensionId(realDim));
      player.getConnection().delayedWrite(joinGame);
      player.getConnection().delayedWrite(
          new Respawn(realDim, joinGame.getPartialHashedSeed(), joinGame.getDifficulty(),
              joinGame.getGamemode(), joinGame.getLevelType()));
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
    if (!player.getKnownChannels().isEmpty()) {
      serverMc.delayedWrite(constructChannelsPacket(serverVersion, player.getKnownChannels()));
    }

    // If we had plugin messages queued during login/FML handshake, send them now.
    PluginMessage pm;
    while ((pm = loginPluginMessages.poll()) != null) {
      serverMc.delayedWrite(pm);
    }

    // Clear any title from the previous server.
    if (player.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      player.getConnection()
          .delayedWrite(TitlePacket.resetForProtocolVersion(player.getProtocolVersion()));
    }

    // Flush everything
    player.getConnection().flush();
    serverMc.flush();
    destination.completeJoin();
  }

  private static int getFakeTemporaryDimensionId(int dim) {
    return dim == 0 ? -1 : 0;
  }

  public List<UUID> getServerBossBars() {
    return serverBossBars;
  }

  private boolean handleCommandTabComplete(TabCompleteRequest packet) {
    // In 1.13+, we need to do additional work for the richer suggestions available.
    String command = packet.getCommand().substring(1);
    int commandEndPosition = command.indexOf(' ');
    if (commandEndPosition == -1) {
      commandEndPosition = command.length();
    }

    String commandLabel = command.substring(0, commandEndPosition);
    if (!server.getCommandManager().hasCommand(commandLabel)) {
      if (player.getProtocolVersion().compareTo(MINECRAFT_1_13) < 0) {
        // Outstanding tab completes are recorded for use with 1.12 clients and below to provide
        // additional tab completion support.
        outstandingTabComplete = packet;
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
      player.getConnection().write(resp);
    }
    return true;
  }

  private boolean handleRegularTabComplete(TabCompleteRequest packet) {
    if (player.getProtocolVersion().compareTo(MINECRAFT_1_13) < 0) {
      // Outstanding tab completes are recorded for use with 1.12 clients and below to provide
      // additional tab completion support.
      outstandingTabComplete = packet;
    }
    return false;
  }

  /**
   * Handles additional tab complete.
   *
   * @param response the tab complete response from the backend
   */
  public void handleTabCompleteResponse(TabCompleteResponse response) {
    if (outstandingTabComplete != null && !outstandingTabComplete.isAssumeCommand()) {
      if (outstandingTabComplete.getCommand().startsWith("/")) {
        this.finishCommandTabComplete(outstandingTabComplete, response);
      } else {
        this.finishRegularTabComplete(outstandingTabComplete, response);
      }
      outstandingTabComplete = null;
    } else {
      // Nothing to do
      player.getConnection().write(response);
    }
  }

  private void finishCommandTabComplete(TabCompleteRequest request, TabCompleteResponse response) {
    String command = request.getCommand().substring(1);
    try {
      List<String> offers = server.getCommandManager().offerSuggestions(player, command);
      for (String offer : offers) {
        response.getOffers().add(new Offer(offer, null));
      }
      response.getOffers().sort(null);
      player.getConnection().write(response);
    } catch (Exception e) {
      logger.error("Unable to provide tab list completions for {} for command '{}'",
          player.getUsername(),
          command, e);
    }
  }

  private void finishRegularTabComplete(TabCompleteRequest request, TabCompleteResponse response) {
    List<String> offers = new ArrayList<>();
    for (Offer offer : response.getOffers()) {
      offers.add(offer.getText());
    }
    server.getEventManager().fire(new TabCompleteEvent(player, request.getCommand(), offers))
        .thenAcceptAsync(e -> {
          response.getOffers().clear();
          for (String s : e.getSuggestions()) {
            response.getOffers().add(new Offer(s));
          }
          player.getConnection().write(response);
        }, player.getConnection().eventLoop());
  }

  private CompletableFuture<Void> processCommandExecuteResult(String originalCommand,
      CommandResult result) {
    if (result == CommandResult.denied()) {
      return CompletableFuture.completedFuture(null);
    }

    MinecraftConnection smc = player.ensureAndGetCurrentServer().ensureConnected();
    String commandToRun = result.getCommand().orElse(originalCommand);
    if (result.isForwardToServer()) {
      return CompletableFuture.runAsync(() -> smc.write(Chat.createServerbound("/"
          + commandToRun)), smc.eventLoop());
    } else {
      return server.getCommandManager().executeImmediatelyAsync(player, commandToRun)
          .thenAcceptAsync(hasRun -> {
            if (!hasRun) {
              smc.write(Chat.createServerbound("/" + commandToRun));
            }
          }, smc.eventLoop());
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
