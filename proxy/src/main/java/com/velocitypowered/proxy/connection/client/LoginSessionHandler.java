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

package com.velocitypowered.proxy.connection.client;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;
import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;
import static com.velocitypowered.proxy.util.EncryptionUtils.decryptRsa;
import static com.velocitypowered.proxy.util.EncryptionUtils.generateServerId;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.event.permission.PermissionsSetupEventImpl;
import com.velocitypowered.api.event.player.DisconnectEvent.LoginStatus;
import com.velocitypowered.api.event.player.DisconnectEventImpl;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEventImpl;
import com.velocitypowered.api.event.player.LoginEventImpl;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEventImpl;
import com.velocitypowered.api.event.player.PostLoginEventImpl;
import com.velocitypowered.api.event.player.PreLoginEvent;
import com.velocitypowered.api.event.player.PreLoginEventImpl;
import com.velocitypowered.api.permission.PermissionFunction;
import com.velocitypowered.api.proxy.player.java.JavaPlayerIdentity;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundEncryptionRequestPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundServerLoginSuccessPacket;
import com.velocitypowered.proxy.network.java.packet.clientbound.ClientboundSetCompressionPacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundEncryptionResponsePacket;
import com.velocitypowered.proxy.network.java.packet.serverbound.ServerboundServerLoginPacket;
import com.velocitypowered.proxy.network.java.states.ProtocolStates;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

public class LoginSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(LoginSessionHandler.class);
  private static final String MOJANG_HASJOINED_URL =
      "https://sessionserver.mojang.com/session/minecraft/hasJoined?username=%s&serverId=%s";

  private final VelocityServer server;
  private final MinecraftConnection mcConnection;
  private final InitialInboundConnection inbound;
  private @MonotonicNonNull ServerboundServerLoginPacket login;
  private byte[] verify = EMPTY_BYTE_ARRAY;
  private @MonotonicNonNull ConnectedPlayer connectedPlayer;

  LoginSessionHandler(VelocityServer server, MinecraftConnection mcConnection,
      InitialInboundConnection inbound) {
    this.server = Preconditions.checkNotNull(server, "server");
    this.mcConnection = Preconditions.checkNotNull(mcConnection, "mcConnection");
    this.inbound = Preconditions.checkNotNull(inbound, "inbound");
  }

  @Override
  public boolean handle(ServerboundServerLoginPacket packet) {
    this.login = packet;
    beginPreLogin();
    return true;
  }

  @Override
  public boolean handle(ServerboundEncryptionResponsePacket packet) {
    ServerboundServerLoginPacket login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    if (verify.length == 0) {
      throw new IllegalStateException("No EncryptionRequest packet sent yet.");
    }

    try {
      KeyPair serverKeyPair = server.getServerKeyPair();
      byte[] decryptedVerifyToken = decryptRsa(serverKeyPair, packet.getVerifyToken());
      if (!MessageDigest.isEqual(verify, decryptedVerifyToken)) {
        throw new IllegalStateException("Unable to successfully decrypt the verification token.");
      }

      byte[] decryptedSharedSecret = decryptRsa(serverKeyPair, packet.getSharedSecret());
      String serverId = generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

      String url = String.format(MOJANG_HASJOINED_URL,
          urlFormParameterEscaper().escape(login.getUsername()), serverId);

      if (server.configuration().shouldPreventClientProxyConnections()) {
        SocketAddress playerRemoteAddress = mcConnection.getRemoteAddress();
        if (playerRemoteAddress instanceof InetSocketAddress) {
          url += "&ip=" + urlFormParameterEscaper().escape(
              ((InetSocketAddress) playerRemoteAddress).getHostString());
        }
      }

      ListenableFuture<Response> hasJoinedResponse = server.getAsyncHttpClient().prepareGet(url)
          .execute();
      hasJoinedResponse.addListener(() -> {
        if (mcConnection.isClosed()) {
          // The player disconnected after we authenticated them.
          return;
        }

        // Go ahead and enable encryption. Once the client sends EncryptionResponse, encryption
        // is enabled.
        try {
          mcConnection.enableEncryption(decryptedSharedSecret);
        } catch (GeneralSecurityException e) {
          throw new RuntimeException(e);
        }

        try {
          Response profileResponse = hasJoinedResponse.get();
          if (profileResponse.getStatusCode() == 200) {
            // All went well, initialize the session.
            initializePlayer(GENERAL_GSON.fromJson(profileResponse.getResponseBody(),
                JavaPlayerIdentity.class), true);
          } else if (profileResponse.getStatusCode() == 204) {
            // Apparently an offline-mode user logged onto this online-mode proxy.
            inbound.disconnect(Component.translatable("velocity.error.online-mode-only",
                NamedTextColor.RED));
          } else {
            // Something else went wrong
            logger.error(
                "Got an unexpected error code {} whilst contacting Mojang to log in {}",
                profileResponse.getStatusCode(), login.getUsername());
            mcConnection.close(true);
          }
        } catch (ExecutionException e) {
          logger.error("Unable to authenticate with Mojang", e);
          mcConnection.close(true);
        } catch (InterruptedException e) {
          // not much we can do usefully
          Thread.currentThread().interrupt();
        }
      }, mcConnection.eventLoop());
    } catch (GeneralSecurityException e) {
      logger.error("Unable to enable encryption", e);
      mcConnection.close(true);
    }
    return true;
  }

  private void beginPreLogin() {
    ServerboundServerLoginPacket login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }
    PreLoginEvent event = new PreLoginEventImpl(inbound, login.getUsername(),
        server.configuration().isOnlineMode());
    server.eventManager().fire(event)
        .thenRunAsync(() -> {
          if (mcConnection.isClosed()) {
            // The player was disconnected
            return;
          }

          ComponentResult result = event.result();
          Component disconnectReason = result.reason();
          if (disconnectReason != null) {
            // The component is guaranteed to be provided if the connection was denied.
            inbound.disconnect(disconnectReason);
            return;
          }

          if (event.onlineMode()) {
            // Request encryption.
            ClientboundEncryptionRequestPacket request = generateEncryptionRequest();
            this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
            mcConnection.write(request);
          } else {
            initializePlayer(JavaPlayerIdentity.forOfflinePlayer(login.getUsername()), false);
          }
        }, mcConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception in pre-login stage", ex);
          return null;
        });
  }

  private ClientboundEncryptionRequestPacket generateEncryptionRequest() {
    byte[] verify = new byte[4];
    ThreadLocalRandom.current().nextBytes(verify);

    ClientboundEncryptionRequestPacket request = new ClientboundEncryptionRequestPacket();
    request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
    request.setVerifyToken(verify);
    return request;
  }

  private void initializePlayer(JavaPlayerIdentity profile, boolean onlineMode) {
    // Some connection types may need to alter the game profile.
    profile = mcConnection.getType().addGameProfileTokensIfRequired(profile,
        server.configuration().getPlayerInfoForwardingMode());
    GameProfileRequestEvent profileRequestEvent = new GameProfileRequestEventImpl(inbound, profile,
        onlineMode);
    final JavaPlayerIdentity finalProfile = profile;

    server.eventManager().fire(profileRequestEvent).thenComposeAsync(profileEvent -> {
      if (mcConnection.isClosed()) {
        // The player disconnected after we authenticated them.
        return CompletableFuture.completedFuture(null);
      }

      // Initiate a regular connection and move over to it.
      ConnectedPlayer player = new ConnectedPlayer(server, profileEvent.gameProfile(),
          mcConnection, inbound.connectedHostname(), onlineMode);
      this.connectedPlayer = player;
      if (!server.canRegisterConnection(player)) {
        player.disconnect0(Component.translatable("velocity.error.already-connected-proxy",
            NamedTextColor.RED), true);
        return CompletableFuture.completedFuture(null);
      }

      logger.info("{} has connected", player);

      return server.eventManager()
          .fire(new PermissionsSetupEventImpl(player, ConnectedPlayer.DEFAULT_PERMISSIONS))
          .thenAcceptAsync(event -> {
            if (!mcConnection.isClosed()) {
              // wait for permissions to load, then set the players permission function
              final PermissionFunction function = event.createFunction(player);
              if (function == null) {
                logger.error(
                    "A plugin permission provider {} provided an invalid permission function"
                        + " for player {}. This is a bug in the plugin, not in Velocity. Falling"
                        + " back to the default permission function.",
                    event.provider().getClass().getName(),
                    player.username());
              } else {
                player.setPermissionFunction(function);
              }
              completeLoginProtocolPhaseAndInitialize(player);
            }
          }, mcConnection.eventLoop());
    }, mcConnection.eventLoop()).exceptionally((ex) -> {
      logger.error("Exception during connection of {}", finalProfile, ex);
      return null;
    });  
  }

  private void completeLoginProtocolPhaseAndInitialize(ConnectedPlayer player) {
    int threshold = server.configuration().getCompressionThreshold();
    if (threshold >= 0 && mcConnection.getProtocolVersion().gte(MINECRAFT_1_8)) {
      mcConnection.write(new ClientboundSetCompressionPacket(threshold));
      mcConnection.setCompressionThreshold(threshold);
    }
    VelocityConfiguration configuration = server.configuration();
    UUID playerUniqueId = player.id();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
      playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.username());
    }
    mcConnection.write(new ClientboundServerLoginSuccessPacket(playerUniqueId, player.username()));

    mcConnection.setAssociation(player);
    mcConnection.setState(ProtocolStates.PLAY);

    server.eventManager().fire(new LoginEventImpl(player))
        .thenAcceptAsync(event -> {
          if (mcConnection.isClosed()) {
            // The player was disconnected
            server.eventManager().fireAndForget(new DisconnectEventImpl(player,
                LoginStatus.CANCELLED_BY_USER_BEFORE_COMPLETE));
            return;
          }

          Component denialReason = event.result().reason();
          if (denialReason != null) {
            player.disconnect0(denialReason, true);
          } else {
            if (!server.registerConnection(player)) {
              player.disconnect0(Component.translatable("velocity.error.already-connected-proxy"),
                  true);
              return;
            }

            mcConnection.setSessionHandler(new InitialConnectSessionHandler(player));
            server.eventManager().fire(new PostLoginEventImpl(player))
                .thenCompose((ignored) -> connectToInitialServer(player))
                .exceptionally((ex) -> {
                  logger.error("Exception while connecting {} to initial server", player, ex);
                  return null;
                });
          }
        }, mcConnection.eventLoop())
        .exceptionally((ex) -> {
          logger.error("Exception while completing login initialisation phase for {}", player, ex);
          return null;
        });
  }

  private CompletableFuture<Void> connectToInitialServer(ConnectedPlayer player) {
    RegisteredServer initialFromConfig = player.getNextServerToTry();
    PlayerChooseInitialServerEvent event = new PlayerChooseInitialServerEventImpl(player,
        initialFromConfig);

    return server.eventManager().fire(event)
        .thenRunAsync(() -> {
          RegisteredServer toTry = event.initialServer();
          if (toTry == null) {
            player.disconnect0(Component.translatable("velocity.error.no-available-servers",
                NamedTextColor.RED), true);
            return;
          }
          player.createConnectionRequest(toTry).fireAndForget();
        }, mcConnection.eventLoop());
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    mcConnection.close(true);
  }

  @Override
  public void disconnected() {
    if (connectedPlayer != null) {
      connectedPlayer.teardown();
    }
  }
}
