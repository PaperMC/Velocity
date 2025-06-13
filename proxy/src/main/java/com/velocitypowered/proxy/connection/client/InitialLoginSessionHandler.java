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

import static com.velocitypowered.proxy.connection.VelocityConstants.EMPTY_BYTE_ARRAY;
import static com.velocitypowered.proxy.crypto.EncryptionUtils.decryptRsa;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Longs;
import com.velocitypowered.api.event.connection.AuthAttemptEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.crypto.IdentifiedKeyImpl;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import com.velocitypowered.proxy.util.VelocityProperties;
import io.netty.buffer.ByteBuf;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;

/**
 * Handles authenticating the player to Mojang's servers.
 */
public class InitialLoginSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(InitialLoginSessionHandler.class);

  private final VelocityServer server;
  private final MinecraftConnection mcConnection;
  private final LoginInboundConnection inbound;
  private @MonotonicNonNull ServerLoginPacket login;
  private byte[] verify = EMPTY_BYTE_ARRAY;
  private LoginState currentState = LoginState.LOGIN_PACKET_EXPECTED;
  private boolean onlineMode;
  private final boolean offlineEncryption;
  private final boolean forceKeyAuthentication;

  InitialLoginSessionHandler(VelocityServer server, MinecraftConnection mcConnection,
                             LoginInboundConnection inbound) {
    this.server = Preconditions.checkNotNull(server, "server");
    this.mcConnection = Preconditions.checkNotNull(mcConnection, "mcConnection");
    this.inbound = Preconditions.checkNotNull(inbound, "inbound");
    this.onlineMode = server.getConfiguration().isOnlineMode();
    this.offlineEncryption = server.getConfiguration().isOfflineEncryptionEnabled();
    this.forceKeyAuthentication = VelocityProperties.readBoolean(
            "auth.forceSecureProfiles", server.getConfiguration().isForceKeyAuthentication());
  }

  @Override
  public boolean handle(ServerLoginPacket packet) {
    assertState(LoginState.LOGIN_PACKET_EXPECTED);
    this.currentState = LoginState.LOGIN_PACKET_RECEIVED;
    IdentifiedKey playerKey = packet.getPlayerKey();
    if (playerKey != null) {
      if (playerKey.hasExpired()) {
        inbound.disconnect(
            Component.translatable("multiplayer.disconnect.invalid_public_key_signature"));
        return true;
      }

      boolean isKeyValid;
      if (playerKey.getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
          && playerKey instanceof final IdentifiedKeyImpl keyImpl) {
        isKeyValid = keyImpl.internalAddHolder(packet.getHolderUuid());
      } else {
        isKeyValid = playerKey.isSignatureValid();
      }

      if (!isKeyValid) {
        inbound.disconnect(Component.translatable("multiplayer.disconnect.invalid_public_key"));
        return true;
      }
    } else if (mcConnection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_19)
        && forceKeyAuthentication
        && mcConnection.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
      inbound.disconnect(Component.translatable("multiplayer.disconnect.missing_public_key"));
      return true;
    }
    inbound.setPlayerKey(playerKey);
    this.login = packet;

    String username = login.getUsername();
    PreLoginEvent event = new PreLoginEvent(inbound, username, login.getHolderUuid(), offlineEncryption);

    server.getEventManager().fire(event).thenRunAsync(() -> {
      if (mcConnection.isClosed()) {
        // The player was disconnected
        return;
      }

      PreLoginComponentResult result = event.getResult();
      Optional<Component> disconnectReason = result.getReasonComponent();
      if (disconnectReason.isPresent()) {
        // The component is guaranteed to be provided if the connection was denied.
        inbound.disconnect(disconnectReason.get());
        return;
      }

      inbound.loginEventFired(() -> {
        if (mcConnection.isClosed()) {
          // The player was disconnected
          return;
        }

        mcConnection.eventLoop().execute(() -> {
          if (result.isForceOfflineMode()) {
            onlineMode = false;
          } else if (result.isOnlineModeAllowed()) {
            onlineMode = true;
          }

          // support for offline mode encryption was added in 1.20.5
          if (onlineMode || (event.isOfflineEncryption() &&
              mcConnection.getProtocolVersion().noLessThan(ProtocolVersion.MINECRAFT_1_20_5))) {
            EncryptionRequestPacket request = generateEncryptionRequest();
            request.setShouldAuthenticate(onlineMode);

            this.verify = Arrays.copyOf(request.getVerifyToken(), 4);
            mcConnection.write(request);
            this.currentState = LoginState.ENCRYPTION_REQUEST_SENT;
          } else {
            // fire event and handle result on connection event loop
            server.getEventManager().fire(new AuthAttemptEvent(
                inbound, false, EMPTY_BYTE_ARRAY, username
            )).thenAcceptAsync(e -> {
              if (!mcConnection.isClosed()) {
                AuthAttemptEvent.AuthResult res = e.getResult();
                if (res == null) {
                  // we allow offline mode connections if no auth result
                  res = new AuthAttemptEvent.SuccessResult(GameProfile.forOfflinePlayer(username));
                }

                handlePlainAuthResult(res);
              }
            }, mcConnection.eventLoop());
          }
        });
      });
    }, mcConnection.eventLoop()).exceptionally((ex) -> {
      logger.error("Exception in pre-login stage", ex);
      return null;
    });

    return true;
  }

  @Override
  public boolean handle(LoginPluginResponsePacket packet) {
    this.inbound.handleLoginPluginResponse(packet);
    return true;
  }

  @Override
  public boolean handle(EncryptionResponsePacket packet) {
    assertState(LoginState.ENCRYPTION_REQUEST_SENT);
    this.currentState = LoginState.ENCRYPTION_RESPONSE_RECEIVED;
    ServerLoginPacket login = this.login;
    if (login == null) {
      throw new IllegalStateException("No ServerLogin packet received yet.");
    }

    if (verify.length == 0) {
      throw new IllegalStateException("No EncryptionRequest packet sent yet.");
    }

    byte[] sharedSecret;
    try {
      KeyPair serverKeyPair = server.getServerKeyPair();
      if (inbound.getIdentifiedKey() != null) {
        IdentifiedKey playerKey = inbound.getIdentifiedKey();
        if (!playerKey.verifyDataSignature(packet.getVerifyToken(), verify,
            Longs.toByteArray(packet.getSalt()))) {
          throw new IllegalStateException("Invalid client public signature.");
        }
      } else {
        byte[] decryptedVerifyToken = decryptRsa(serverKeyPair, packet.getVerifyToken());
        if (!MessageDigest.isEqual(verify, decryptedVerifyToken)) {
          throw new IllegalStateException("Unable to successfully decrypt the verification token.");
        }
      }

      sharedSecret = decryptRsa(serverKeyPair, packet.getSharedSecret());
    } catch (GeneralSecurityException e) {
      logger.error("Unable to enable encryption", e);
      mcConnection.close(true);
      return true;
    }

    // fire event and handle result on connection event loop
    server.getEventManager().fire(new AuthAttemptEvent(
        inbound, onlineMode, sharedSecret, login.getUsername()
    )).thenAcceptAsync(event -> {
      if (!mcConnection.isClosed()) {
        AuthAttemptEvent.AuthResult res = event.getResult();
        if (res == null) {
          // if no auth result, we deny online mode connections but allow offline mode connections
          res = onlineMode ? new AuthAttemptEvent.FailureResult(
              Component.translatable("multiplayer.disconnect.authservers_down")) :
              new AuthAttemptEvent.SuccessResult(
                  GameProfile.forOfflinePlayer(login.getUsername())
              );
        }

        handleEncryptedAuthResult(res, sharedSecret);
      }
    }, mcConnection.eventLoop()).exceptionally(ex -> {
      logger.error("Exception in authentication attempt", ex);
      return null;
    });

    return true;
  }

  private void handlePlainAuthResult(AuthAttemptEvent.AuthResult result) {
    if (result instanceof AuthAttemptEvent.SuccessResult res) {
      mcConnection.setActiveSessionHandler(StateRegistry.LOGIN,
          new AuthSessionHandler(server, inbound, res.profile(), false));
    } else if (result instanceof AuthAttemptEvent.FailureResult failure) {
      inbound.disconnect(failure.reason());
    }
  }

  private void handleEncryptedAuthResult(AuthAttemptEvent.AuthResult res, byte[] sharedSecret) {
    if (res instanceof AuthAttemptEvent.SuccessResult result) {
      // verify public key for 1.19.1+
      if (inbound.getIdentifiedKey() != null
          && inbound.getIdentifiedKey().getKeyRevision() == IdentifiedKey.Revision.LINKED_V2
          && inbound.getIdentifiedKey() instanceof final IdentifiedKeyImpl key) {
        if (!key.internalAddHolder(result.profile().getId())) {
          inbound.disconnect(
              Component.translatable("multiplayer.disconnect.invalid_public_key"));
          return;
        }
      }

      // enable encryption for the connection
      try {
        mcConnection.enableEncryption(sharedSecret);
      } catch (GeneralSecurityException e) {
        logger.error("Unable to enable encryption for connection", e);
        mcConnection.close(true);
        return;
      }

      // initialize the session
      mcConnection.setActiveSessionHandler(StateRegistry.LOGIN,
          new AuthSessionHandler(server, inbound, result.profile(), true));
    } else if (res instanceof AuthAttemptEvent.FailureResult failure) {
      // disconnect the player with the provided reason
      inbound.disconnect(failure.reason());
    } else if (res == null) {
      inbound.disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
    }
  }

  private EncryptionRequestPacket generateEncryptionRequest() {
    byte[] verify = new byte[4];
    ThreadLocalRandom.current().nextBytes(verify);

    EncryptionRequestPacket request = new EncryptionRequestPacket();
    request.setPublicKey(server.getServerKeyPair().getPublic().getEncoded());
    request.setVerifyToken(verify);
    return request;
  }

  @Override
  public void handleUnknown(ByteBuf buf) {
    mcConnection.close(true);
  }

  @Override
  public void disconnected() {
    this.inbound.cleanup();
  }

  private void assertState(LoginState expectedState) {
    if (this.currentState != expectedState) {
      if (MinecraftDecoder.DEBUG) {
        logger.error("{} Received an unexpected packet requiring state {}, but we are in {}",
            inbound,
            expectedState, this.currentState);
      }
      mcConnection.close(true);
    }
  }

  private enum LoginState {
    LOGIN_PACKET_EXPECTED,
    LOGIN_PACKET_RECEIVED,
    ENCRYPTION_REQUEST_SENT,
    ENCRYPTION_RESPONSE_RECEIVED
  }
}
