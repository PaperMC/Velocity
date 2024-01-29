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

package com.velocitypowered.proxy.connection;

import static com.velocitypowered.proxy.VelocityServer.GENERAL_GSON;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.UnaryOperator;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"MissingJavadocMethod", "MissingJavadocType"})
public final class PlayerDataForwarding {
  private static final String ALGORITHM = "HmacSHA256";

  public static final String CHANNEL = "velocity:player_info";

  public static final int MODERN_DEFAULT = 1;
  public static final int MODERN_WITH_KEY = 2;
  public static final int MODERN_WITH_KEY_V2 = 3;
  public static final int MODERN_LAZY_SESSION = 4;
  public static final int MODERN_MAX_VERSION = MODERN_LAZY_SESSION;

  private static final char LEGACY_SEPARATOR = '\0';

  private static final String BUNGEE_GUARD_TOKEN_PROPERTY_NAME = "bungeeguard-token";

  private PlayerDataForwarding() {
  }

  public static ByteBuf createForwardingData(
      final byte[] secret,
      final String address,
      final ProtocolVersion protocol,
      final GameProfile profile,
      final @Nullable IdentifiedKey key,
      final int requestedVersion
  ) {
    final ByteBuf forwarded = Unpooled.buffer(2048);
    try {
      final int actualVersion = findForwardingVersion(requestedVersion, protocol, key);

      ProtocolUtils.writeVarInt(forwarded, actualVersion);
      ProtocolUtils.writeString(forwarded, address);
      ProtocolUtils.writeUuid(forwarded, profile.getId());
      ProtocolUtils.writeString(forwarded, profile.getName());
      ProtocolUtils.writeProperties(forwarded, profile.getProperties());

      // This serves as additional redundancy. The key normally is stored in the
      // login start to the server, but some setups require this.
      if (actualVersion >= MODERN_WITH_KEY
          && actualVersion < MODERN_LAZY_SESSION) {
        assert key != null;
        ProtocolUtils.writePlayerKey(forwarded, key);

        // Provide the signer UUID since the UUID may differ from the
        // assigned UUID. Doing that breaks the signatures anyway but the server
        // should be able to verify the key independently.
        if (actualVersion >= MODERN_WITH_KEY_V2) {
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

      final Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      mac.update(forwarded.array(), forwarded.arrayOffset(), forwarded.readableBytes());
      final byte[] sig = mac.doFinal();

      return Unpooled.wrappedBuffer(Unpooled.wrappedBuffer(sig), forwarded);
    } catch (final InvalidKeyException e) {
      forwarded.release();
      throw new RuntimeException("Unable to authenticate data", e);
    } catch (final NoSuchAlgorithmException e) {
      // Should never happen
      forwarded.release();
      throw new AssertionError(e);
    }
  }

  private static int findForwardingVersion(
      int requested,
      final ProtocolVersion protocol,
      final @Nullable IdentifiedKey key
  ) {
    // Ensure we are in range
    requested = Math.min(requested, MODERN_MAX_VERSION);
    if (requested > MODERN_DEFAULT) {
      if (protocol.noLessThan(ProtocolVersion.MINECRAFT_1_19_3)) {
        return requested >= MODERN_LAZY_SESSION
            ? MODERN_LAZY_SESSION
            : MODERN_DEFAULT;
      }
      if (key != null) {
        return switch (key.getKeyRevision()) {
          case GENERIC_V1 -> MODERN_WITH_KEY;
          // Since V2 is not backwards compatible we have to throw the key if v2 and requested is v1
          case LINKED_V2 -> requested >= MODERN_WITH_KEY_V2
                  ? MODERN_WITH_KEY_V2
                  : MODERN_DEFAULT;
        };
      } else {
        return MODERN_DEFAULT;
      }
    }
    return MODERN_DEFAULT;
  }

  public static String createLegacyForwardingAddress(
      final String serverAddress,
      final String playerAddress,
      final GameProfile profile
  ) {
    return createLegacyForwardingAddress(
        serverAddress,
        playerAddress,
        profile,
        UnaryOperator.identity()
    );
  }

  private static String createLegacyForwardingAddress(
      final String serverAddress,
      final String playerAddress,
      final GameProfile profile,
      final UnaryOperator<List<GameProfile.Property>> propertiesTransform
  ) {
    // BungeeCord IP forwarding is simply a special injection after the "address" in the handshake,
    // separated by \0 (the null byte). In order, you send the original host, the player's IP, their
    // UUID (undashed), and if you are in online-mode, their login properties (from Mojang).
    final StringBuilder data = new StringBuilder()
        .append(serverAddress)
        .append(LEGACY_SEPARATOR)
        .append(playerAddress)
        .append(LEGACY_SEPARATOR)
        .append(profile.getUndashedId())
        .append(LEGACY_SEPARATOR);
    GENERAL_GSON
        .toJson(propertiesTransform.apply(profile.getProperties()), data);
    return data.toString();
  }

  public static String createBungeeGuardForwardingAddress(
      final String serverAddress,
      final String playerAddress,
      final GameProfile profile,
      final byte[] forwardingSecret
  ) {
    // Append forwarding secret as a BungeeGuard token.
    final GameProfile.Property property = new GameProfile.Property(
        BUNGEE_GUARD_TOKEN_PROPERTY_NAME,
        new String(forwardingSecret, StandardCharsets.UTF_8),
        ""
    );
    return createLegacyForwardingAddress(
        serverAddress,
        playerAddress,
        profile,
        properties -> ImmutableList.<GameProfile.Property>builder()
            .addAll(properties)
            .add(property)
            .build()
    );
  }
}
