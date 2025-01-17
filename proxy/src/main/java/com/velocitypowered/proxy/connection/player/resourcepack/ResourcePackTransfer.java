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

package com.velocitypowered.proxy.connection.player.resourcepack;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.util.NettyPreconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.DecoderException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;

/**
 * Utility class for handling resource pack transfer operations in Velocity.
 *
 * <p>This class provides methods for creating and validating signed cookie data for applied
 * resource packs. These cookies are used to securely store and transfer information about
 * resource packs between the client and server, ensuring data integrity and authenticity.</p>
 *
 * <p>The signing mechanism uses the HMAC-SHA256 algorithm, with a secret key to produce and
 * verify signatures. This ensures that tampered or invalid data is detected.</p>
 *
 * <h3>Usage:</h3>
 * <pre>
 * // Creating cookie data
 * byte[] secretKey = ...; // Secret key for signing
 * Collection&lt;ResourcePackInfo&gt; resourcePacks = ...; // Applied resource packs
 * byte[] cookieData = ResourcePackTransfer.createCookieData(secretKey, resourcePacks);
 *
 * // Decoding and validating cookie data
 * try {
 *     Collection&lt;ResourcePackInfo&gt; validatedPacks =
 *         ResourcePackTransfer.decodeAndValidateCookieData(secretKey, cookieData);
 * } catch (SignatureException | DecoderException e) {
 *     // Handle invalid or tampered cookie data
 * }
 * </pre>
 *
 * <p>Instances of this class cannot be created. All methods are static.</p>
 *
 * @see ResourcePackInfo
 */
public class ResourcePackTransfer {
  public static Key APPLIED_RESOURCE_PACKS_KEY = Key.key("velocity", "applied_resource_packs");

  private static final String ALGORITHM = "HmacSHA256";
  private static ResourcePackInfo.Origin[] ORIGINS = ResourcePackInfo.Origin.values();

  private ResourcePackTransfer() {
  }

  /**
   * Creates a signed cookie data byte array for the given applied resource packs.
   *
   * <p>This method serializes the resource pack information into a compact binary format, signs it
   * using HMAC-SHA256 with the provided secret key, and returns the resulting byte array. The
   * signature ensures the integrity and authenticity of the data.</p>
   *
   * @param secret               the secret key used for signing the cookie data
   * @param appliedResourcePacks a collection of {@link ResourcePackInfo} objects representing
   *                             the applied resource packs
   * @return a byte array containing the signed cookie data
   * @throws RuntimeException if the HMAC-SHA256 algorithm is not available or the secret key
   *                          is invalid
   */
  public static byte[] createCookieData(final byte[] secret, final Collection<ResourcePackInfo> appliedResourcePacks) {
    if (appliedResourcePacks.isEmpty()) {
      return new byte[0];
    }

    final ByteBuf buffer = Unpooled.buffer(appliedResourcePacks.size() * 256);
    try {
      ProtocolUtils.writeVarInt(buffer, appliedResourcePacks.size());
      for (ResourcePackInfo appliedResourcePack : appliedResourcePacks) {
        ProtocolUtils.writeString(buffer, appliedResourcePack.getUrl());
        ProtocolUtils.writeUuid(buffer, appliedResourcePack.getId());
        byte[] hash = appliedResourcePack.getHash();
        buffer.writeBoolean(hash != null);
        if (hash != null) {
          buffer.writeBytes(hash);
        }
        buffer.writeBoolean(appliedResourcePack.getShouldForce());
        Component prompt = appliedResourcePack.getPrompt();
        buffer.writeBoolean(prompt != null);
        if (prompt != null) {
          ProtocolUtils.writeString(buffer, ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MAXIMUM_VERSION).serialize(prompt));
        }
        ProtocolUtils.writeVarInt(buffer, appliedResourcePack.getOrigin().ordinal());
        ProtocolUtils.writeVarInt(buffer, appliedResourcePack.getOriginalOrigin().ordinal());
      }

      final Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      mac.update(buffer.array(), buffer.arrayOffset(), buffer.readableBytes());
      buffer.writeBytes(mac.doFinal());

      return Arrays.copyOfRange(buffer.array(), buffer.arrayOffset(), buffer.arrayOffset() + buffer.readableBytes());
    } catch (final InvalidKeyException e) {
      buffer.release();
      throw new RuntimeException("Unable to sign applied resource packs cookie data", e);
    } catch (final NoSuchAlgorithmException e) {
      // Should never happen
      throw new AssertionError(e);
    } finally {
      buffer.release();
    }
  }

  /**
   * Decodes and validates the given cookie data byte array.
   *
   * <p>This method checks the integrity and authenticity of the cookie data by verifying its
   * HMAC-SHA256 signature using the provided secret key. If the signature is valid, it deserializes
   * the data back into a collection of {@link ResourcePackInfo} objects. If the data is invalid
   * or tampered with, a {@link SignatureException} is thrown.</p>
   *
   * @param secret the secret key used to verify the cookie data's signature
   * @param data   the cookie data byte array to decode and validate
   * @return a collection of {@link ResourcePackInfo} objects representing the applied resource packs
   * @throws SignatureException if the signature is missing, incomplete, or invalid
   * @throws DecoderException   if there is an error during decoding
   * @throws RuntimeException   if the HMAC-SHA256 algorithm is not available or the secret key
   *                            is invalid
   */
  public static Collection<ResourcePackInfo> decodeAndValidateCookieData(final byte[] secret, final byte[] data)
      throws SignatureException, DecoderException {
    if (data == null || data.length == 0) {
      return Collections.emptyList();
    }
    if (data.length <= 32) {
      throw new SignatureException("Applied resource packs cookie data has no or incomplete signature");
    }

    try {
      final Mac mac = Mac.getInstance(ALGORITHM);
      mac.init(new SecretKeySpec(secret, ALGORITHM));
      mac.update(data, 0, data.length - 32);

      if (!Arrays.equals(mac.doFinal(), 0, 32, data, data.length - 32, data.length)) {
        throw new SignatureException("Applied resource packs cookie data has invalid signature");
      }
    } catch (final InvalidKeyException e) {
      throw new RuntimeException("Unable to verify signature of applied resource packs cookie data", e);
    } catch (final NoSuchAlgorithmException e) {
      // Should never happen
      throw new AssertionError(e);
    }

    ByteBuf buffer = Unpooled.wrappedBuffer(data);
    try {
      int size = ProtocolUtils.readVarInt(buffer);
      NettyPreconditions.checkFrame(size <= 256, "Too many applied packs (got %s, maximum is %s)", size, 256);
      List<ResourcePackInfo> appliedResourcePacks = new ArrayList<>(size);
      for (int i = 0; i < size; i++) {
        VelocityResourcePackInfo.BuilderImpl builder = new VelocityResourcePackInfo.BuilderImpl(ProtocolUtils.readString(buffer));
        builder.setId(ProtocolUtils.readUuid(buffer));
        if (buffer.readBoolean()) {
          byte[] hash = new byte[20];
          buffer.readBytes(hash);
          builder.setHash(hash);
        }
        builder.setShouldForce(buffer.readBoolean());
        if (buffer.readBoolean()) {
          builder.setPrompt(ProtocolUtils.getJsonChatSerializer(ProtocolVersion.MAXIMUM_VERSION).deserialize(ProtocolUtils.readString(buffer)));
        }
        builder.setOrigin(ORIGINS[ProtocolUtils.readVarInt(buffer)]);
        VelocityResourcePackInfo appliedResourcePack = builder.build();
        appliedResourcePack.setOriginalOrigin(ORIGINS[ProtocolUtils.readVarInt(buffer)]);
        appliedResourcePacks.add(appliedResourcePack);
        buffer.readBoolean();
      }
      return appliedResourcePacks;
    } finally {
      buffer.release();
    }
  }
}
