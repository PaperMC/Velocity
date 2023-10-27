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

import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.connection.VelocityConstants;
import com.velocitypowered.proxy.connection.client.ClientPlaySessionHandler;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults;
import com.velocitypowered.proxy.connection.util.ConnectionRequestResults.Impl;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packet.LoginAcknowledged;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.util.except.QuietRuntimeException;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CompletableFuture;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import net.kyori.adventure.text.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
  public boolean handle(EncryptionRequest packet) {
    throw new IllegalStateException("Backend server is online-mode!");
  }

  @Override
  public boolean handle(LoginPluginMessage packet) {
    MinecraftConnection mc = serverConn.ensureConnected();
    VelocityConfiguration configuration = server.getConfiguration();
    if (configuration.getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && packet.getChannel().equals(VelocityConstants.VELOCITY_IP_FORWARDING_CHANNEL)) {

      int requestedForwardingVersion = VelocityConstants.MODERN_FORWARDING_DEFAULT;
      // Check version
      if (packet.content().readableBytes() == 1) {
        requestedForwardingVersion = packet.content().readByte();
      }
      ByteBuf forwardingData = createForwardingData(configuration.getForwardingSecret(),
          serverConn.getPlayerRemoteAddressAsString(), serverConn.getPlayer(),
          requestedForwardingVersion);

      LoginPluginResponse response = new LoginPluginResponse(packet.getId(), true, forwardingData);
      mc.write(response);
      informationForwarded = true;
    } else {
      // Don't understand, fire event if we have subscribers
      if (!this.server.getEventManager().hasSubscribers(ServerLoginPluginMessageEvent.class)) {
        mc.write(new LoginPluginResponse(packet.getId(), false, Unpooled.EMPTY_BUFFER));
        return true;
      }

      final byte[] contents = ByteBufUtil.getBytes(packet.content());
      final MinecraftChannelIdentifier identifier = MinecraftChannelIdentifier
          .from(packet.getChannel());
      this.server.getEventManager().fire(new ServerLoginPluginMessageEvent(serverConn, identifier,
              contents, packet.getId()))
          .thenAcceptAsync(event -> {
            if (event.getResult().isAllowed()) {
              mc.write(new LoginPluginResponse(packet.getId(), true, Unpooled
                  .wrappedBuffer(event.getResult().getResponse())));
            } else {
              mc.write(new LoginPluginResponse(packet.getId(), false, Unpooled.EMPTY_BUFFER));
            }
          }, mc.eventLoop());
    }
    return true;
  }

  @Override
  public boolean handle(Disconnect packet) {
    resultFuture.complete(ConnectionRequestResults.forDisconnect(packet, serverConn.getServer()));
    serverConn.disconnect();
    return true;
  }

  @Override
  public boolean handle(SetCompression packet) {
    serverConn.ensureConnected().setCompressionThreshold(packet.getThreshold());
    return true;
  }

  @Override
  public boolean handle(ServerLoginSuccess packet) {
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.MODERN
        && !informationForwarded) {
      resultFuture.complete(ConnectionRequestResults.forDisconnect(MODERN_IP_FORWARDING_FAILURE,
          serverConn.getServer()));
      serverConn.disconnect();
      return true;
    }

    // The player has been logged on to the backend server, but we're not done yet. There could be
    // other problems that could arise before we get a JoinGame packet from the server.

    // Move into the PLAY phase.
    MinecraftConnection smc = serverConn.ensureConnected();
    if (smc.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_20_2) < 0) {
      smc.setActiveSessionHandler(StateRegistry.PLAY,
          new TransitionSessionHandler(server, serverConn, resultFuture));
    } else {
      smc.write(new LoginAcknowledged());
      smc.setActiveSessionHandler(StateRegistry.CONFIG,
          new ConfigSessionHandler(server, serverConn, resultFuture));
      ConnectedPlayer player = serverConn.getPlayer();
      if (player.getClientSettingsPacket() != null) {
        smc.write(player.getClientSettingsPacket());
      }
      if (player.getConnection().getActiveSessionHandler() instanceof ClientPlaySessionHandler) {
        smc.setAutoReading(false);
        ((ClientPlaySessionHandler) player.getConnection()
            .getActiveSessionHandler()).doSwitch().thenAcceptAsync((unused) -> {
              smc.setAutoReading(true);
            }, smc.eventLoop());
      }
    }

    return true;
  }

  @Override
  public void exception(Throwable throwable) {
    resultFuture.completeExceptionally(throwable);
  }

  @Override
  public void disconnected() {
    if (server.getConfiguration().getPlayerInfoForwardingMode() == PlayerInfoForwarding.LEGACY) {
      resultFuture.completeExceptionally(new QuietRuntimeException(
          "The connection to the remote server was unexpectedly closed.\n"
              + "This is usually because the remote server "
              + "does not have BungeeCord IP forwarding "
              + "correctly enabled.\nSee https://velocitypowered.com/wiki/users/forwarding/ "
              + "for instructions on how to configure player info forwarding correctly."));
    } else {
      resultFuture.completeExceptionally(
          new QuietRuntimeException("The connection to the remote server was unexpectedly closed.")
      );
    }
  }

  private static int findForwardingVersion(int requested, ConnectedPlayer player) {
    // Ensure we are in range
    requested = Math.min(requested, VelocityConstants.MODERN_FORWARDING_MAX_VERSION);
    if (requested > VelocityConstants.MODERN_FORWARDING_DEFAULT) {
      if (player.getProtocolVersion().compareTo(ProtocolVersion.MINECRAFT_1_19_3) >= 0) {
        return requested >= VelocityConstants.MODERN_LAZY_SESSION
            ? VelocityConstants.MODERN_LAZY_SESSION
            : VelocityConstants.MODERN_FORWARDING_DEFAULT;
      }
      if (player.getIdentifiedKey() != null) {
        // No enhanced switch on java 11
        switch (player.getIdentifiedKey().getKeyRevision()) {
          case GENERIC_V1:
            return VelocityConstants.MODERN_FORWARDING_WITH_KEY;
          // Since V2 is not backwards compatible we have to throw the key if v2 and requested is v1
          case LINKED_V2:
            return requested >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                ? VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2
                : VelocityConstants.MODERN_FORWARDING_DEFAULT;
          default:
            return VelocityConstants.MODERN_FORWARDING_DEFAULT;
        }
      } else {
        return VelocityConstants.MODERN_FORWARDING_DEFAULT;
      }
    }
    return VelocityConstants.MODERN_FORWARDING_DEFAULT;
  }

  private static ByteBuf createForwardingData(byte[] hmacSecret, String address,
      ConnectedPlayer player, int requestedVersion) {
    ByteBuf forwarded = Unpooled.buffer(2048);
    try {
      int actualVersion = findForwardingVersion(requestedVersion, player);

      ProtocolUtils.writeVarInt(forwarded, actualVersion);
      ProtocolUtils.writeString(forwarded, address);
      ProtocolUtils.writeUuid(forwarded, player.getGameProfile().getId());
      ProtocolUtils.writeString(forwarded, player.getGameProfile().getName());
      ProtocolUtils.writeProperties(forwarded, player.getGameProfile().getProperties());

      // This serves as additional redundancy. The key normally is stored in the
      // login start to the server, but some setups require this.
      if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY
          && actualVersion < VelocityConstants.MODERN_LAZY_SESSION) {
        IdentifiedKey key = player.getIdentifiedKey();
        assert key != null;
        ProtocolUtils.writePlayerKey(forwarded, key);

        // Provide the signer UUID since the UUID may differ from the
        // assigned UUID. Doing that breaks the signatures anyway but the server
        // should be able to verify the key independently.
        if (actualVersion >= VelocityConstants.MODERN_FORWARDING_WITH_KEY_V2) {
          if (key.getSignatureHolder() != null) {
            forwarded.writeBoolean(true);
            ProtocolUtils.writeUuid(forwarded, key.getSignatureHolder());
          } else {
            // Should only not be provided if the player was connected
            // as offline-mode and the signer UUID was not backfilled
            forwarded.writeBoolean(false);
          }
        }
      }

      SecretKey key = new SecretKeySpec(hmacSecret, "HmacSHA256");
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(key);
      mac.update(forwarded.array(), forwarded.arrayOffset(), forwarded.readableBytes());
      byte[] sig = mac.doFinal();

      return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded);
    } catch (InvalidKeyException e) {
      forwarded.release();
      throw new RuntimeException("Unable to authenticate data", e);
    } catch (NoSuchAlgorithmException e) {
      // Should never happen
      forwarded.release();
      throw new AssertionError(e);
    }
  }
}
