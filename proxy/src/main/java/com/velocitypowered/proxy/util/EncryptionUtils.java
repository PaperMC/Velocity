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

package com.velocitypowered.proxy.util;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import javax.crypto.Cipher;

public enum EncryptionUtils {
  ;

  /**
   * Generates an RSA key pair.
   *
   * @param keysize the key size (in bits) for the RSA key pair
   * @return the generated key pair
   */
  public static KeyPair createRsaKeyPair(final int keysize) {
    try {
      final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(keysize);
      return generator.generateKeyPair();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to generate RSA keypair", e);
    }
  }

  /**
   * Generates a hex digest in two's complement form for use with the Mojang joinedServer endpoint.
   *
   * @param digest the bytes to digest
   * @return the hex digest
   */
  public static String twosComplementHexdigest(byte[] digest) {
    return new BigInteger(digest).toString(16);
  }

  /**
   * Decrypts an RSA message.
   *
   * @param keyPair the key pair to use
   * @param bytes the bytes of the encrypted message
   * @return the decrypted message
   * @throws GeneralSecurityException if the message couldn't be decoded
   */
  public static byte[] decryptRsa(KeyPair keyPair, byte[] bytes) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
    return cipher.doFinal(bytes);
  }

  /**
   * Generates the server ID for the hasJoined endpoint.
   *
   * @param sharedSecret the shared secret between the client and the proxy
   * @param key the RSA public key
   * @return the server ID
   */
  public static String generateServerId(byte[] sharedSecret, PublicKey key) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(sharedSecret);
      digest.update(key.getEncoded());
      return twosComplementHexdigest(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
