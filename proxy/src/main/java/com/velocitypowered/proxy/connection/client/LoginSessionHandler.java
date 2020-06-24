package com.velocitypowered.proxy.connection.client;

import static com.google.common.net.UrlEscapers.urlFormParameterEscaper;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.proxy.VelocityServer.GSON;
import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;
import static com.velocitypowered.proxy.util.EncryptionUtils.decryptRsa;
import static com.velocitypowered.proxy.util.EncryptionUtils.generateServerId;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.event.permission.PermissionsSetupEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponse;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.util.VelocityMessages;
import io.netty.buffer.ByteBuf;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.text.Component;
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
  private @MonotonicNonNull ServerLogin login;
  private byte[] verify = EMPTY_BYTE_ARRAY;
  private @MonotonicNonNull ConnectedPlayer connectedPlayer;

  LoginSessionHandler(VelocityServer server, MinecraftConnection mcConnection,
      InitialInboundConnection inbound) {
    this.server = Preconditions.checkNotNull(server, "server");
    this.mcConnection = Preconditions.checkNotNull(mcConnection, "mcConnection");
    this.inbound = Preconditions.checkNotNull(inbound, "inbound");
  }

  @Override
  public boolean handle(ServerLogin packet) {
    this.login = packet;
    beginPreLogin();
    return true;
  }

  @Override
  public boolean handle(EncryptionResponse packet) {
    ServerLogin login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    if (verify.length == 0) {
      throw new IllegalStateException("No EncryptionRequest packet sent yet.");
    }

    try {
      KeyPair serverKeyPair = server.getServerKeyPair();
      byte[] decryptedVerifyToken = decryptRsa(serverKeyPair, packet.getVerifyToken());
      if (!Arrays.equals(verify, decryptedVerifyToken)) {
        throw new IllegalStateException("Unable to successfully decrypt the verification token.");
      }

      byte[] decryptedSharedSecret = decryptRsa(serverKeyPair, packet.getSharedSecret());
      String serverId = generateServerId(decryptedSharedSecret, serverKeyPair.getPublic());

      String playerIp = ((InetSocketAddress) mcConnection.getRemoteAddress()).getHostString();
      String url = String.format(MOJANG_HASJOINED_URL,
          urlFormParameterEscaper().escape(login.getUsername()), serverId);

      if (server.getConfiguration().shouldPreventClientProxyConnections()) {
        url += "&ip=" + urlFormParameterEscaper().escape(playerIp);
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
            initializePlayer(GSON.fromJson(profileResponse.getResponseBody(), GameProfile.class),
                true);
          } else if (profileResponse.getStatusCode() == 204) {
            // Apparently an offline-mode user logged onto this online-mode proxy.
            inbound.disconnect(VelocityMessages.ONLINE_MODE_ONLY);
          } else {
            // Something else went wrong
            logger.error(
                "Got an unexpected error code {} whilst contacting Mojang to log in {} ({})",
                profileResponse.getStatusCode(), login.getUsername(), playerIp);
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
    ServerLogin login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }
    PreLoginEvent event = new PreLoginEvent(inbound, login.getUsername());
    server.getEventManager().fire(event)
        .thenRunAsync(() -> {
          if (mcConnection.isClosed()) {
            // The player was disconnected
            return;
          }

          PreLoginComponentResult result = event.getResult();
          Optional<Component> disconnectReason = result.getReason();
          if (disconnectReason.isPresent()) {
            // The component is guaranteed to be provided if the connection was denied.
            mcConnection.closeWith(Disconnect.create(disconnectReason.get()));
            return;
          }

          if (!result.isForceOfflineMode() && (server.getConfiguration().isOnlineMode() || result
              .isOnlineModeAllowed())) {
            // Request encryption.
            EncryptionRequest request = generateEncryptionRequest();
            this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
            mcConnection.write(request);
          } else {
            initializePlayer(GameProfile.forOfflinePlayer(login.getUsername()), false);
          }
        }, mcConnection.eventLoop());
  }

  private EncryptionRequest generateEncryptionRequest() {
    byte[] verify = new byte[4];
    ThreadLocalRandom.current().nextBytes(verify);

    EncryptionRequest request = new EncryptionRequest();
    request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
    request.setVerifyToken(verify);
    return request;
  }

  private void initializePlayer(GameProfile profile, boolean onlineMode) {
    // Some connection types may need to alter the game profile.
    profile = mcConnection.getType().addGameProfileTokensIfRequired(profile,
        server.getConfiguration().getPlayerInfoForwardingMode());
    GameProfileRequestEvent profileRequestEvent = new GameProfileRequestEvent(inbound, profile,
        onlineMode);

    server.getEventManager().fire(profileRequestEvent).thenCompose(profileEvent -> {
      // Initiate a regular connection and move over to it.
      ConnectedPlayer player = new ConnectedPlayer(server, profileEvent.getGameProfile(),
          mcConnection,
          inbound.getVirtualHost().orElse(null), onlineMode);
      this.connectedPlayer = player;
      if (!server.canRegisterConnection(player)) {
        player.disconnect0(VelocityMessages.ALREADY_CONNECTED, true);
        return CompletableFuture.completedFuture(null);
      }

      logger.info("{} has connected", player);

      return server.getEventManager()
          .fire(new PermissionsSetupEvent(player, ConnectedPlayer.DEFAULT_PERMISSIONS))
          .thenAcceptAsync(event -> {
            if (!mcConnection.isClosed()) {
              // wait for permissions to load, then set the players permission function
              player.setPermissionFunction(event.createFunction(player));
              finishLogin(player);
            }
          }, mcConnection.eventLoop());
    });  
  }

  private void finishLogin(ConnectedPlayer player) {
    int threshold = server.getConfiguration().getCompressionThreshold();
    if (threshold >= 0 && mcConnection.getProtocolVersion().compareTo(MINECRAFT_1_8) >= 0) {
      mcConnection.write(new SetCompression(threshold));
      mcConnection.setCompressionThreshold(threshold);
    }
    VelocityConfiguration configuration = server.getConfiguration();
    UUID playerUniqueId = player.getUniqueId();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.NONE) {
      playerUniqueId = UuidUtils.generateOfflinePlayerUuid(player.getUsername());
    }
    ServerLoginSuccess success = new ServerLoginSuccess();
    success.setUsername(player.getUsername());
    success.setUuid(playerUniqueId);
    mcConnection.write(success);

    mcConnection.setAssociation(player);
    mcConnection.setState(StateRegistry.PLAY);

    server.getEventManager().fire(new LoginEvent(player))
        .thenAcceptAsync(event -> {
          if (mcConnection.isClosed()) {
            // The player was disconnected
            return;
          }

          Optional<Component> reason = event.getResult().getReason();
          if (reason.isPresent()) {
            player.disconnect0(reason.get(), true);
          } else {
            if (!server.registerConnection(player)) {
              player.disconnect0(VelocityMessages.ALREADY_CONNECTED, true);
              return;
            }

            mcConnection.setSessionHandler(new InitialConnectSessionHandler(player));
            server.getEventManager().fire(new PostLoginEvent(player))
                .thenRun(() -> connectToInitialServer(player));
          }
        }, mcConnection.eventLoop());
  }

  private void connectToInitialServer(ConnectedPlayer player) {
    Optional<RegisteredServer> initialFromConfig = player.getNextServerToTry();
    PlayerChooseInitialServerEvent event = new PlayerChooseInitialServerEvent(player,
        initialFromConfig.orElse(null));

    server.getEventManager().fire(event)
        .thenRunAsync(() -> {
          Optional<RegisteredServer> toTry = event.getInitialServer();
          if (!toTry.isPresent()) {
            player.disconnect0(VelocityMessages.NO_AVAILABLE_SERVERS, true);
            return;
          }
          player.createConnectionRequest(toTry.get()).fireAndForget();
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
