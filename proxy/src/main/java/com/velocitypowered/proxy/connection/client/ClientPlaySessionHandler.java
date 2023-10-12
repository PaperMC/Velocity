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

import static com.velocitypowered.proxy.protocol.util.PluginMessageUtil.constructChannelsPacket;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.suggestion.Suggestion;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChannelRegisterEvent;
import com.velocitypowered.api.event.player.PlayerClientBrandEvent;
import com.velocitypowered.api.event.player.TabCompleteEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.ConnectionTypes;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.backend.BackendConnectionPhases;
import com.velocitypowered.proxy.connection.backend.BungeeCordMessageResponder;
import com.velocitypowered.proxy.connection.backend.VelocityServerConnection;
import com.velocitypowered.proxy.connection.forge.legacy.LegacyForgeConstants;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse.Offer;
import com.velocitypowered.proxy.protocol.packet.chat.ChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.ChatTimeKeeper;
import com.velocitypowered.proxy.protocol.packet.chat.CommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedCommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.keyed.KeyedPlayerCommand;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChat;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.legacy.LegacyCommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionChatHandler;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionCommandHandler;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerChat;
import com.velocitypowered.proxy.protocol.packet.chat.session.SessionPlayerCommand;
import com.velocitypowered.proxy.protocol.packet.config.FinishedUpdate;
import com.velocitypowered.proxy.protocol.packet.title.GenericTitlePacket;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import com.velocitypowered.proxy.util.CharacterUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
  private final Queue<PluginMessage> loginPluginMessages = new ConcurrentLinkedQueue<>();
  private final VelocityServer server;
  private @Nullable TabCompleteRequest outstandingTabComplete;
  private final ChatHandler<? extends MinecraftPacket> chatHandler;
  private final CommandHandler<? extends MinecraftPacket> commandHandler;
  private final ChatTimeKeeper timeKeeper = new ChatTimeKeeper();

  private CompletableFuture<Void> configSwitchFuture;

  /**
   * Constructs a client play session handler.
   *
   * @param server the Velocity server instance
   * @param player the player
   */
  public ClientPlaySessionHandler(VelocityServer server, ConnectedPlayer player) {
    this.player = player;
    this.server = server;

    if (this.player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
      this.chatHandler = new SessionChatHandler(this.player, this.server);
      this.commandHandler = new SessionCommandHandler(this.player, this.server);
    } else if (this.player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19) >= 0) {
      this.chatHandler = new KeyedChatHandler(this.server, this.player);
      this.commandHandler = new KeyedCommandHandler(this.player, this.server);
    } else {
      this.chatHandler = new LegacyChatHandler(this.server, this.player);
      this.commandHandler = new LegacyCommandHandler(this.player, this.server);
    }
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean updateTimeKeeper(@Nullable Instant instant) {
    if (instant == null) {
      return true;
    }
    if (!this.timeKeeper.update(instant)) {
      player.disconnect(Component.translatable("multiplayer.disconnect.out_of_order_chat"));
      return false;
    }
    return true;
  }

  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  private boolean validateChat(String message) {
    if (CharacterUtil.containsIllegalCharacters(message)) {
      player.disconnect(
          Component.translatable("velocity.error.illegal-chat-characters", NamedTextColor.RED));
      return false;
    }
    return true;
  }

  @Override
  public void activated() {
    configSwitchFuture = new CompletableFuture<>();
    Collection<String> channels =
        server.getChannelRegistrar().getChannelsForProtocol(player.getProtocolVersion());
    if (!channels.isEmpty()) {
      PluginMessage register = constructChannelsPacket(player.getProtocolVersion(), channels);
      player.getConnection().write(register);
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
    if (serverConnection != null) {
      Long sentTime = serverConnection.getPendingPings().remove(packet.getRandomId());
      if (sentTime != null) {
        MinecraftConnection smc = serverConnection.getConnection();
        if (smc != null) {
          player.setPing(System.currentTimeMillis() - sentTime);
          smc.write(packet);
        }
      }
    }
    return true;
  }

  @Override
  public boolean handle(ClientSettings packet) {
    player.setPlayerSettings(packet);
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection == null) {
      // No server connection yet, probably transitioning.
      return true;
    }
    player.getConnectedServer().ensureConnected().write(packet);
    return true; // will forward onto the server
  }

  @Override
  public boolean handle(SessionPlayerCommand packet) {
    player.ensureAndGetCurrentServer();

    if (!updateTimeKeeper(packet.getTimeStamp())) {
      return true;
    }

    if (!validateChat(packet.getCommand())) {
      return true;
    }

    return this.commandHandler.handlePlayerCommand(packet);
  }

  @Override
  public boolean handle(SessionPlayerChat packet) {
    player.ensureAndGetCurrentServer();

    if (!updateTimeKeeper(packet.getTimestamp())) {
      return true;
    }

    if (!validateChat(packet.getMessage())) {
      return true;
    }

    return this.chatHandler.handlePlayerChat(packet);
  }

  @Override
  public boolean handle(KeyedPlayerCommand packet) {
    player.ensureAndGetCurrentServer();

    if (!updateTimeKeeper(packet.getTimestamp())) {
      return true;
    }

    if (!validateChat(packet.getCommand())) {
      return true;
    }

    return this.commandHandler.handlePlayerCommand(packet);
  }

  @Override
  public boolean handle(KeyedPlayerChat packet) {
    player.ensureAndGetCurrentServer();

    if (!updateTimeKeeper(packet.getExpiry())) {
      return true;
    }

    if (!validateChat(packet.getMessage())) {
      return true;
    }

    return this.chatHandler.handlePlayerChat(packet);
  }

  @Override
  public boolean handle(LegacyChat packet) {
    player.ensureAndGetCurrentServer();
    String msg = packet.getMessage();
    if (!validateChat(msg)) {
      return true;
    }

    if (msg.startsWith("/")) {
      this.commandHandler.handlePlayerCommand(packet);
    } else {
      this.chatHandler.handlePlayerChat(packet);
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
    // Handling edge case when packet with FML client handshake (state COMPLETE)
    // arrives after JoinGame packet from destination server
    VelocityServerConnection serverConn =
            (player.getConnectedServer() == null
                    && packet.getChannel().equals(
                            LegacyForgeConstants.FORGE_LEGACY_HANDSHAKE_CHANNEL))
            ? player.getConnectionInFlight() : player.getConnectedServer();

    MinecraftConnection backendConn = serverConn != null ? serverConn.getConnection() : null;
    if (serverConn != null && backendConn != null) {
      if (backendConn.getState() != StateRegistry.PLAY) {
        logger.warn("A plugin message was received while the backend server was not "
            + "ready. Channel: {}. Packet discarded.", packet.getChannel());
      } else if (PluginMessageUtil.isRegister(packet)) {
        List<String> channels = PluginMessageUtil.getChannels(packet);
        List<ChannelIdentifier> channelIdentifiers = new ArrayList<>();
        for (String channel : channels) {
          try {
            channelIdentifiers.add(MinecraftChannelIdentifier.from(channel));
          } catch (IllegalArgumentException e) {
            channelIdentifiers.add(new LegacyChannelIdentifier(channel));
          }
        }
        server.getEventManager()
            .fireAndForget(
                new PlayerChannelRegisterEvent(player, ImmutableList.copyOf(channelIdentifiers)));
        backendConn.write(packet.retain());
      } else if (PluginMessageUtil.isUnregister(packet)) {
        backendConn.write(packet.retain());
      } else if (PluginMessageUtil.isMcBrand(packet)) {
        String brand = PluginMessageUtil.readBrandMessage(packet.content());
        server.getEventManager().fireAndForget(new PlayerClientBrandEvent(player, brand));
        player.setClientBrand(brand);
        backendConn.write(
            PluginMessageUtil.rewriteMinecraftBrand(packet, server.getVersion(),
                player.getProtocolVersion()));
      } else if (BungeeCordMessageResponder.isBungeeCordMessage(packet)) {
        return true;
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
          ChannelIdentifier id = server.getChannelRegistrar().getFromId(packet.getChannel());
          if (id == null) {
            // We don't have any plugins listening on this channel, process the packet now.
            if (!player.getPhase().consideredComplete() || !serverConn.getPhase()
                .consideredComplete()) {
              // The client is trying to send messages too early. This is primarily caused by mods,
              // but further aggravated by Velocity. To work around these issues, we will queue any
              // non-FML handshake messages to be sent once the FML handshake has completed or the
              // JoinGame packet has been received by the proxy, whichever comes first.
              //
              // We also need to make sure to retain these packets, so they can be flushed
              // appropriately.
              loginPluginMessages.add(packet.retain());
            } else {
              // The connection is ready, send the packet now.
              backendConn.write(packet.retain());
            }
          } else {
            byte[] copy = ByteBufUtil.getBytes(packet.content());
            PluginMessageEvent event = new PluginMessageEvent(player, serverConn, id, copy);
            server.getEventManager().fire(event).thenAcceptAsync(pme -> {
              if (pme.getResult().isAllowed()) {
                PluginMessage message = new PluginMessage(packet.getChannel(),
                    Unpooled.wrappedBuffer(copy));
                if (!player.getPhase().consideredComplete() || !serverConn.getPhase()
                    .consideredComplete()) {
                  // We're still processing the connection (see above), enqueue the packet for now.
                  loginPluginMessages.add(message.retain());
                } else {
                  backendConn.write(message);
                }
              }
            }, backendConn.eventLoop()).exceptionally((ex) -> {
              logger.error("Exception while handling plugin message packet for {}", player, ex);
              return null;
            });
          }
        }
      }
    }

    return true;
  }

  @Override
  public boolean handle(ResourcePackResponse packet) {
    return player.onResourcePackResponse(packet.getStatus());
  }

  @Override
  public boolean handle(FinishedUpdate packet) {
    // Complete client switch
    player.getConnection().setActiveSessionHandler(StateRegistry.CONFIG);
    VelocityServerConnection serverConnection = player.getConnectedServer();
    if (serverConnection != null) {
      MinecraftConnection smc = serverConnection.ensureConnected();
      CompletableFuture.runAsync(() -> {
        smc.write(packet);
        smc.setActiveSessionHandler(StateRegistry.CONFIG);
        smc.setAutoReading(true);
      }, smc.eventLoop()).exceptionally((ex) -> {
        logger.error("Error forwarding config state acknowledgement to server:", ex);
        return null;
      });
    }
    configSwitchFuture.complete(null);
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
    player.disconnect(
        Component.translatable("velocity.error.player-connection-error", NamedTextColor.RED));
  }

  @Override
  public void writabilityChanged() {
    boolean writable = player.getConnection().getChannel().isWritable();

    if (!writable) {
      // We might have packets queued from the server, so flush them now to free up memory. Make
      // sure to do it on a future invocation of the event loop, otherwise while the issue will
      // fix itself, we'll still disable auto-reading and instead of backpressure resolution, we
      // get client timeouts.
      player.getConnection().eventLoop().execute(() -> player.getConnection().flush());
    }

    VelocityServerConnection serverConn = player.getConnectedServer();
    if (serverConn != null) {
      MinecraftConnection smc = serverConn.getConnection();
      if (smc != null) {
        smc.setAutoReading(writable);
      }
    }
  }

  /**
   * Handles switching stages for swapping between servers.
   *
   * @return a future that completes when the switch is complete
   */
  public CompletableFuture<Void> doSwitch() {
    VelocityServerConnection existingConnection = player.getConnectedServer();

    if (existingConnection != null) {
      // Shut down the existing server connection.
      player.setConnectedServer(null);
      existingConnection.disconnect();

      // Send keep alive to try to avoid timeouts
      player.sendKeepAlive();

      // Config state clears everything in the client. No need to clear later.
      spawned = false;
      serverBossBars.clear();
      player.clearPlayerListHeaderAndFooterSilent();
      player.getTabList().clearAllSilent();
    }

    player.switchToConfigState();

    return configSwitchFuture;
  }

  /**
   * Handles the {@code JoinGame} packet. This function is responsible for handling the client-side
   * switching servers in Velocity.
   *
   * @param joinGame    the join game packet
   * @param destination the new server we are connecting to
   */
  public void handleBackendJoinGame(JoinGame joinGame, VelocityServerConnection destination) {
    final MinecraftConnection serverMc = destination.ensureConnected();

    if (!spawned) {
      // The player wasn't spawned in yet, so we don't need to do anything special. Just send
      // JoinGame.
      spawned = true;
      player.getConnection().delayedWrite(joinGame);
      // Required for Legacy Forge
      player.getPhase().onFirstJoin(player);
    } else {
      // Clear tab list to avoid duplicate entries
      player.getTabList().clearAll();

      // The player is switching from a server already, so we need to tell the client to change
      // entity IDs and send new dimension information.
      if (player.getConnection().getType() == ConnectionTypes.LEGACY_FORGE) {
        this.doSafeClientServerSwitch(joinGame);
      } else {
        this.doFastClientServerSwitch(joinGame);
      }
    }

    destination.setActiveDimensionRegistry(joinGame.getRegistry()); // 1.16

    // Remove previous boss bars. These don't get cleared when sending JoinGame, thus the need to
    // track them.
    for (UUID serverBossBar : serverBossBars) {
      BossBar deletePacket = new BossBar();
      deletePacket.setUuid(serverBossBar);
      deletePacket.setAction(BossBar.REMOVE);
      player.getConnection().delayedWrite(deletePacket);
    }
    serverBossBars.clear();

    // Tell the server about the proxy's plugin message channels.
    ProtocolVersion serverVersion = serverMc.getProtocolVersion();
    final Collection<String> channels = server.getChannelRegistrar()
            .getChannelsForProtocol(serverMc.getProtocolVersion());
    if (!channels.isEmpty()) {
      serverMc.delayedWrite(constructChannelsPacket(serverVersion, channels));
    }

    // If we had plugin messages queued during login/FML handshake, send them now.
    PluginMessage pm;
    while ((pm = loginPluginMessages.poll()) != null) {
      serverMc.delayedWrite(pm);
    }

    // Clear any title from the previous server.
    if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_8) >= 0) {
      player.getConnection().delayedWrite(
          GenericTitlePacket.constructTitlePacket(GenericTitlePacket.ActionType.RESET,
              player.getProtocolVersion()));
    }

    // Flush everything
    player.getConnection().flush();
    serverMc.flush();
    destination.completeJoin();
  }

  private void doFastClientServerSwitch(JoinGame joinGame) {
    // In order to handle switching to another server, you will need to send two packets:
    //
    // - The join game packet from the backend server, with a different dimension
    // - A respawn with the correct dimension
    //
    // Most notably, by having the client accept the join game packet, we can work around the need
    // to perform entity ID rewrites, eliminating potential issues from rewriting packets and
    // improving compatibility with mods.
    final Respawn respawn = Respawn.fromJoinGame(joinGame);

    if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_16) < 0) {
      // Before Minecraft 1.16, we could not switch to the same dimension without sending an
      // additional respawn. On older versions of Minecraft this forces the client to perform
      // garbage collection which adds additional latency.
      joinGame.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    }
    player.getConnection().delayedWrite(joinGame);
    player.getConnection().delayedWrite(respawn);
  }

  private void doSafeClientServerSwitch(JoinGame joinGame) {
    // Some clients do not behave well with the "fast" respawn sequence. In this case we will use
    // a "safe" respawn sequence that involves sending three packets to the client. They have the
    // same effect but tend to work better with buggier clients (Forge 1.8 in particular).

    // Send the JoinGame packet itself, unmodified.
    player.getConnection().delayedWrite(joinGame);

    // Send a respawn packet in a different dimension.
    final Respawn fakeSwitchPacket = Respawn.fromJoinGame(joinGame);
    fakeSwitchPacket.setDimension(joinGame.getDimension() == 0 ? -1 : 0);
    player.getConnection().delayedWrite(fakeSwitchPacket);

    // Now send a respawn packet in the correct dimension.
    final Respawn correctSwitchPacket = Respawn.fromJoinGame(joinGame);
    player.getConnection().delayedWrite(correctSwitchPacket);
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
      if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
        // Outstanding tab completes are recorded for use with 1.12 clients and below to provide
        // additional tab completion support.
        outstandingTabComplete = packet;
      }
      return false;
    }

    server.getCommandManager().offerBrigadierSuggestions(player, command)
        .thenAcceptAsync(suggestions -> {
          if (suggestions.isEmpty()) {
            return;
          }

          List<Offer> offers = new ArrayList<>();
          for (Suggestion suggestion : suggestions.getList()) {
            String offer = suggestion.getText();
            Component tooltip = null;
            if (suggestion.getTooltip() != null
                && suggestion.getTooltip() instanceof VelocityBrigadierMessage) {
              tooltip = ((VelocityBrigadierMessage) suggestion.getTooltip()).asComponent();
            }
            offers.add(new Offer(offer, tooltip));
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
        }, player.getConnection().eventLoop()).exceptionally((ex) -> {
          logger.error("Exception while handling command tab completion for player {} executing {}",
              player, command, ex);
          return null;
        });
    return true; // Sorry, handler; we're just gonna have to lie to you here.
  }

  private boolean handleRegularTabComplete(TabCompleteRequest packet) {
    if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0) {
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
    server.getCommandManager().offerBrigadierSuggestions(player, command)
        .thenAcceptAsync(offers -> {
          boolean legacy =
              player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_13) < 0;
          try {
            for (Suggestion suggestion : offers.getList()) {
              String offer = suggestion.getText();
              offer = legacy && !offer.startsWith("/") ? "/" + offer : offer;
              if (legacy && offer.startsWith(command)) {
                offer = offer.substring(command.length());
              }
              Component tooltip = null;
              if (suggestion.getTooltip() != null
                  && suggestion.getTooltip() instanceof VelocityBrigadierMessage) {
                tooltip = ((VelocityBrigadierMessage) suggestion.getTooltip()).asComponent();
              }
              response.getOffers().add(new Offer(offer, tooltip));
            }
            response.getOffers().sort(null);
            player.getConnection().write(response);
          } catch (Exception e) {
            logger.error("Unable to provide tab list completions for {} for command '{}'",
                player.getUsername(), command,
                e);
          }
        }, player.getConnection().eventLoop()).exceptionally((ex) -> {
          logger.error(
              "Exception while finishing command tab completion,"
                  + " with request {} and response {}",
              request, response, ex);
          return null;
        });
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
        }, player.getConnection().eventLoop()).exceptionally((ex) -> {
          logger.error(
              "Exception while finishing regular tab completion,"
                  + " with request {} and response{}",
              request, response, ex);
          return null;
        });
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
