/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

package com.velocitypowered.natives.encryption;

import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;

/**
 * A factory interface for {@link VelocityCipher}.
 */
public interface VelocityCipherFactory {

  /**
   * The length of the AES-CFB8 initialization vector, in bytes.
   */
  int IV_LENGTH = 16;

  /**
   * Creates an encrypting cipher.
   *
   * @param key the AES key
   * @param iv the {@value #IV_LENGTH}-byte IV
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  VelocityCipher forEncryption(SecretKey key, byte[] iv) throws GeneralSecurityException;

  /**
   * Creates an encrypting cipher that insecurely uses the key as the IV.
   *
   * @param key the AES key
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   * @deprecated This silently uses the insecure key-as-IV mode. For Minecraft 26.4 and newer, use
   *     {@link #forEncryption(SecretKey, byte[])} with the IV from the key exchange. If you need
   *     the key-as-IV mode for Minecraft 26.3 and older, replace this with
   *     {@link #forEncryptionInsecure(SecretKey)}, which behaves identically.
   */
  @Deprecated
  default VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException {
    return forEncryptionInsecure(key);
  }

  /**
   * Creates a decrypting cipher.
   *
   * @param key the AES key
   * @param iv the {@value #IV_LENGTH}-byte IV
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  VelocityCipher forDecryption(SecretKey key, byte[] iv) throws GeneralSecurityException;

  /**
   * Creates a decrypting cipher that insecurely uses the key as the IV.
   *
   * @param key the AES key
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   * @deprecated This silently uses the insecure key-as-IV mode. For Minecraft 26.4 and newer, use
   *     {@link #forDecryption(SecretKey, byte[])} with the IV from the key exchange. If you need
   *     the key-as-IV mode for Minecraft 26.3 and older, replace this with
   *     {@link #forDecryptionInsecure(SecretKey)}, which behaves identically.
   */
  @Deprecated
  default VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException {
    return forDecryptionInsecure(key);
  }

  /**
   * Creates an encrypting cipher that uses the key as the IV.
   *
   * <p><strong>This is insecure.</strong> Reusing the key as the IV defeats the purpose of an IV,
   * and because the protocol uses the same key in both directions, both streams start out in the
   * same cipher state. It exists only because Minecraft 26.3 and older require it. Minecraft 26.4
   * and newer derive a separate key and IV for each direction: use
   * {@link #forEncryption(SecretKey, byte[])} for those, and for anything else.
   *
   * @param key the AES key
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  default VelocityCipher forEncryptionInsecure(SecretKey key) throws GeneralSecurityException {
    return forEncryption(key, key.getEncoded());
  }

  /**
   * Creates a decrypting cipher that uses the key as the IV.
   *
   * <p><strong>This is insecure.</strong> Reusing the key as the IV defeats the purpose of an IV,
   * and because the protocol uses the same key in both directions, both streams start out in the
   * same cipher state. It exists only because Minecraft 26.3 and older require it. Minecraft 26.4
   * and newer derive a separate key and IV for each direction: use
   * {@link #forDecryption(SecretKey, byte[])} for those, and for anything else.
   *
   * @param key the AES key
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  default VelocityCipher forDecryptionInsecure(SecretKey key) throws GeneralSecurityException {
    return forDecryption(key, key.getEncoded());
  }
}
