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

package com.velocitypowered.proxy.connection.backend;

import com.google.common.primitives.Longs;
import com.velocitypowered.api.event.player.CookieRequestEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.event.player.configuration.PlayerEnteredConfigurationEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.PlayerDataForwarding;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.crypto.EncryptionUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.*;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import fun.iiii.mixedlogin.LoginServerManager;
import fun.iiii.mixedlogin.MixedVelocity;
import fun.iiii.mixedlogin.util.CryptUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.SecretKey;

/**
 * Handles a player trying to log into the proxy.
 */
public class LoginSessionHandler implements MinecraftSessionHandler {

  private static final Logger logger = LogManager.getLogger(LoginSessionHandler.class);

  private static final Component MODERN_IP_FORWARDING_FAILURE =
      Component.translatable("velocity.error.modern-forwarding-failed");

  private final VelocityServer server;
  private final VelocityServerConnection serverConn;
  private final CompletableFuture<Impl> resultFuture;
  private boolean informationForwarded;

  LoginSessionHandler(VelocityServer server, VelocityServerConnection serverConn,
      CompletableFuture<Impl> resultFuture) {
    this.server = server;
    this.serverConn = serverConn;
    this.resultFuture = resultFuture;
  }

  @Override
  public boolean handle(EncryptionRequestPacket packetIn) {
//    throw new IllegalStateException("Backend server is online-mode!");
    final SecretKey secretkey = CryptUtil.createNewSharedKey();
    byte[] sharedSecret=secretkey.getEncoded();
    PublicKey publickey = CryptUtil.decodePublicKey(packetIn.getPublicKey());
    String s = "";
    String serverId = (new BigInteger(CryptUtil.getServerIdHash(s, publickey, secretkey))).toString(16);
    LoginServerManager.getInstance().startSubRequest(serverId,serverConn.getPlayer().getGameProfile());

    byte[] verifyToken=CryptUtil.encryptData(publickey,packetIn.getVerifyToken());
//    生成salt long
    long salt= Longs.fromByteArray(CryptUtil.encryptData(publickey,Longs.toByteArray(System.currentTimeMillis())));
    EncryptionResponsePacket responsePacket=new EncryptionResponsePacket(CryptUtil.encryptData(publickey,sharedSecret),verifyToken,salt);
    serverConn.ensureConnected().write(responsePacket);
    try {
      serverConn.ensureConnected().enableEncryption(sharedSecret);
    } catch (GeneralSecurityException e) {
      logger.error("Unable to enable encryption for connection", e);
      // At this point, the connection is encrypted, but something's wrong on our side and
      // we can't do anything about it.
      serverConn.ensureConnected().close(true);
      return true;
    }
    return true;
  }

  @Override
  public boolean handle(LoginPluginMessagePacket packet) {
    MinecraftConnection mc = serverConn.ensureConnected();
    VelocityConfiguration configuration = server.getConfiguration();
    if (MixedVelocity.getInstance().getForwardingMode(serverConn.getServerInfo().getName()) == PlayerInfoForwarding.MODERN
        && packet.getChannel().equals(PlayerDataForwarding.CHANNEL)) {

      int requestedForwardingVersion = PlayerDataForwarding.MODERN_DEFAULT;
      // Check version
      if (packet.content().readableBytes() == 1) {
        requestedForwardingVersion = packet.content().readByte();
      }
      ConnectedPlayer player = serverConn.getPlayer();
      ByteBuf forwardingData = PlayerDataForwarding.createForwardingData(
          configuration.getForwardingSecret(),
          serverConn.getPlayerRemoteAddressAsString(),
          player.getProtocolVersion(),
          player.getGameProfile(),
          player.getIdentifiedKey(),
          requestedForwardingVersion);

      LoginPluginResponsePacket response = new LoginPluginResponsePacket(
              packet.getId(), true, forwardingData);
      mc.write(response);
      informationForwarded = true;
    } else {
      // Don't understand, fire event if we have subscribers
      if (!this.server.getEventManager().hasSubscribers(ServerLoginPluginMessageEvent.class)) {
        mc.write(new LoginPluginResponsePacket(packet.getId(), false, Unpooled.EMPTY_BUFFER));
        return true;
      }

      final byte[] contents = ByteBufUtil.getBytes(packet.content());
      final MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier
          .from(packet.getChannel());
      this.server.getEventManager().fire(new ServerLoginPluginMessageEvent(serverConn, identifier,
              contents, packet.getId()))
          .thenAcceptAsync(event -> {
            if (event.getResult().isAllowed()) {
              mc.write(new LoginPluginResponsePacket(packet.getId(), true, Unpooled
                  .wrappedBuffer(event.getResult().getResponse())));
            } else {
              mc.write(new LoginPluginResponsePacket(packet.getId(), false, Unpooled.EMPTY_BUFFER));
            }
          }, mc.eventLoop());
    }
    return true;
  }

  @Override
  public boolean handle(DisconnectPacket packet) {
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    serverConn.disconnect();
    return true;
  }

  @Override
  public boolean handle(SetCompressionPacket packet) {
    serverConn.ensureConnected().setCompressionThreshold(packet.getThreshold());
    return true;
  }

  @Override
  public boolean handle(ServerLoginSuccessPacket packet) {
    if (MixedVelocity.getInstance().getForwardingMode(serverConn.getServerInfo().getName()) == PlayerInfoForwarding.MODERN && !informationForwarded) {
      resultFuture.complete(ConnectionRequestResults.forDisconnect(MODERN_IP_FORWARDING_FAILURE, serverConn.getServer()));
      serverConn.disconnect();
      return true;
    }

    // The player has been logged on to the backend server, but we're not done yet. There could be
    // other problems that could arise before we get a JoinGame packet from the server.

    // Move into the PLAY phase.
    MinecraftConnection smc = serverConn.ensureConnected();
    if (smc.getProtocolVersion().lessThan(ProtocolVersion.MINECRAFT_1_20_2)) {
      smc.setActiveSessionHandler(StateRegistry.PLAY, new TransitionSessionHandler(server, serverConn, resultFuture));
    } else {
      smc.write(new LoginAcknowledgedPacket());
      smc.setActiveSessionHandler(StateRegistry.CONFIG, new ConfigSessionHandler(server, serverConn, resultFuture));
      ConnectedPlayer player = serverConn.getPlayer();
      if (player.getClientSettingsPacket() != null) {
        smc.write(player.getClientSettingsPacket());
      }
      if (player.getConnection().getActiveSessionHandler() instanceof ClientPlaySessionHandler clientPlaySessionHandler) {
        smc.setAutoReading(false);
        clientPlaySessionHandler.doSwitch().thenAcceptAsync((unused) -> smc.setAutoReading(true), smc.eventLoop());
      } else {
        // Initial login - the player is already in configuration state.
        server.getEventManager().fireAndForget(new PlayerEnteredConfigurationEvent(player, serverConn));
      }
    }

    return true;
  }

  @Override
  public boolean handle(ClientboundStoreCookiePacket packet) {
    throw new IllegalStateException("Can only store cookie in CONFIGURATION or PLAY protocol");
  }

  @Override
  public boolean handle(ClientboundCookieRequestPacket packet) {
    server.getEventManager().fire(new CookieRequestEvent(serverConn.getPlayer(), packet.getKey()))
        .thenAcceptAsync(event -> {
          if (event.getResult().isAllowed()) {
            final Key resultedKey = event.getResult().getKey() == null
                ? event.getOriginalKey() : event.getResult().getKey();

            serverConn.getPlayer().getConnection().write(new ClientboundCookieRequestPacket(resultedKey));
          }
        }, serverConn.ensureConnected().eventLoop());

    return true;
  }

  @Override
  public void exception(Throwable throwable) {
    resultFuture.completeExceptionally(throwable);
  }

  @Override
  public void disconnected() {
    if (MixedVelocity.getInstance().getForwardingMode(serverConn.getServerInfo().getName()) == PlayerInfoForwarding.LEGACY) {
      resultFuture.completeExceptionally(new QuietRuntimeException(
              """
              The connection to the remote server was unexpectedly closed.
              This is usually because the remote server does not have \
              BungeeCord IP forwarding correctly enabled.
              See https://docs.papermc.io/velocity/player-information-forwarding for instructions \
              on how to configure player info forwarding correctly."""));
    } else {
      resultFuture.completeExceptionally(
          new QuietRuntimeException("The connection to the remote server was unexpectedly closed.")
      );
    }
  }
}
