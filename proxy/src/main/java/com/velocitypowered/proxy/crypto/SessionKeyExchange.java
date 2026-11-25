/*
 * Copyright (C) 2026 Velocity Contributors
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

package com.velocitypowered.proxy.crypto;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.NamedParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.KDF;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.HKDFParameterSpec;

/**
 * The 26.4+ login key exchange: X25519 key agreement, HKDF-SHA256 key derivation, separate AES
 * keys and IVs per direction. The server is side "A", the client side "B". This mirrors vanilla's
 * {@code net.minecraft.util.crypto.SessionKeyExchange} and {@code SessionKeys}.
 */
public final class SessionKeyExchange {

  private static final String KEY_ALGORITHM = "XDH";
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final byte SEPARATOR = '+';
  private static final byte[] SALT = Bytes.concat(
      Longs.toByteArray(3195254534265565831L),
      Longs.toByteArray(8302438179253405948L),
      Longs.toByteArray(6794457759077931664L));
  private static final char[] BASE32_HEX = "0123456789ABCDEFGHIJKLMNOPQRSTUV".toCharArray();

  private SessionKeyExchange() {
    throw new AssertionError();
  }

  /**
   * Generates a new X25519 key pair.
   *
   * @param random the source of randomness
   * @return the key pair
   */
  public static KeyPair generateKeyPair(SecureRandom random) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
      generator.initialize(NamedParameterSpec.X25519, random);
      return generator.generateKeyPair();
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("X25519 is not available", e);
    }
  }

  /**
   * Parses an X.509-encoded X25519 public key.
   *
   * @param encoded the encoded key
   * @return the public key
   * @throws GeneralSecurityException if the key is not a valid X25519 key
   */
  public static PublicKey parsePublicKey(byte[] encoded) throws GeneralSecurityException {
    PublicKey key = KeyFactory.getInstance(KEY_ALGORITHM)
        .generatePublic(new X509EncodedKeySpec(encoded));
    if (!(key.getParams() instanceof NamedParameterSpec spec)
        || !spec.getName().equals(NamedParameterSpec.X25519.getName())) {
      throw new GeneralSecurityException("Wrong curve parameters");
    }
    return key;
  }

  /**
   * Derives the session keys on the server side.
   *
   * @param serverKeyPair the server's X25519 key pair
   * @param serverName the server id sent in the key packet
   * @param clientKey the client's public key
   * @param clientName the username the client logged in with
   * @param protocolVersion the client's protocol version number
   * @param salt the salt sent in the key packet
   * @param authenticate whether the key packet asked the client to authenticate
   * @return the session keys
   * @throws GeneralSecurityException if the key agreement fails
   */
  public static SessionKeys deriveServerSide(KeyPair serverKeyPair, String serverName,
      PublicKey clientKey, String clientName, int protocolVersion, long salt,
      boolean authenticate) throws GeneralSecurityException {
    String connectionParams = "a=" + serverName + ";b=" + clientName + ";protocol="
        + protocolVersion;
    String authParams = "a=" + serverName + ";b=" + clientName;

    KeyAgreement agreement = KeyAgreement.getInstance(KEY_ALGORITHM);
    agreement.init(serverKeyPair.getPrivate(), NamedParameterSpec.X25519);
    agreement.doPhase(clientKey, true);
    byte[] sharedSecret = agreement.generateSecret();

    KDF kdf = KDF.getInstance("HKDF-SHA256");
    HKDFParameterSpec.Builder extract = HKDFParameterSpec.ofExtract()
        .addIKM(sharedSecret)
        .addSalt(Longs.toByteArray(salt))
        .addSalt(SALT);
    final SecretKey serverToClient = kdf.deriveKey("AES",
        extract.thenExpand(utf8("a_to_b;" + connectionParams), 16));
    final SecretKey clientToServer = kdf.deriveKey("AES",
        extract.thenExpand(utf8("b_to_a;" + connectionParams), 16));
    final SecretKey authKey = kdf.deriveKey(HMAC_ALGORITHM,
        extract.thenExpand(utf8("authenticate;" + authParams), 32));
    SecretKey integrityKey = kdf.deriveKey(HMAC_ALGORITHM,
        extract.thenExpand(utf8("verify;" + authParams), 32));

    byte[] serverPublic = serverKeyPair.getPublic().getEncoded();
    byte[] clientPublic = clientKey.getEncoded();

    Mac integrity = Mac.getInstance(HMAC_ALGORITHM);
    integrity.init(integrityKey);
    integrity.update(Ints.toByteArray(protocolVersion));
    integrity.update(SEPARATOR);
    integrity.update(Longs.toByteArray(salt));
    integrity.update(SEPARATOR);
    integrity.update((byte) (authenticate ? 1 : 2));
    integrity.update(SEPARATOR);
    integrity.update(utf8(serverName));
    integrity.update(SEPARATOR);
    integrity.update(serverPublic);
    integrity.update(SEPARATOR);
    integrity.update(utf8(clientName));
    integrity.update(SEPARATOR);
    integrity.update(clientPublic);
    byte[] integrityDigest = integrity.doFinal();

    Mac auth = Mac.getInstance(HMAC_ALGORITHM);
    auth.init(authKey);
    auth.update(serverPublic);
    auth.update(SEPARATOR);
    auth.update(clientPublic);
    String serverId = base32Hex(Arrays.copyOf(auth.doFinal(), 25));

    // Vanilla seeds the IVs from the integrity signature: server->client first, then the other
    Random ivRandom = new Random(Arrays.hashCode(integrityDigest));
    byte[] serverToClientIv = new byte[16];
    byte[] clientToServerIv = new byte[16];
    ivRandom.nextBytes(serverToClientIv);
    ivRandom.nextBytes(clientToServerIv);

    return new SessionKeys(serverToClient, serverToClientIv, clientToServer, clientToServerIv,
        integrityDigest, serverId);
  }

  private static byte[] utf8(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }

  // RFC 4648 base32 with the extended hex alphabet; callers pass whole 5-byte groups (no padding)
  static String base32Hex(byte[] data) {
    StringBuilder out = new StringBuilder(data.length * 8 / 5);
    for (int i = 0; i + 5 <= data.length; i += 5) {
      long group = 0;
      for (int j = 0; j < 5; j++) {
        group = (group << 8) | (data[i + j] & 0xFF);
      }
      for (int shift = 35; shift >= 0; shift -= 5) {
        out.append(BASE32_HEX[(int) ((group >>> shift) & 0x1F)]);
      }
    }
    return out.toString();
  }

  /**
   * The keys of one 26.4+ session, seen from the server.
   *
   * @param serverToClient the outbound stream key
   * @param serverToClientIv the outbound IV
   * @param clientToServer the inbound stream key
   * @param clientToServerIv the inbound IV
   * @param integrityDigest the expected signature in the client's key packet
   * @param serverId the server id to send to the session server's {@code hasJoined}
   */
  public record SessionKeys(SecretKey serverToClient, byte[] serverToClientIv,
                            SecretKey clientToServer, byte[] clientToServerIv,
                            byte[] integrityDigest, String serverId) {
  }
}
