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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A factory interface for {@link VelocityCipher}.
 */
public interface VelocityCipherFactory {

  /**
   * The length of the AES-CFB8 initialization vector, in bytes.
   */
  int IV_LENGTH = 16;

  /**
   * Creates an encrypting cipher that uses the key as the IV, as the pre-26.4 protocol does.
   *
   * @param key the AES key
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  default VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException {
    return forEncryption(key, null);
  }

  /**
   * Creates an encrypting cipher.
   *
   * @param key the AES key
   * @param iv the {@value #IV_LENGTH}-byte IV, or {@code null} to use the key as the IV
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  VelocityCipher forEncryption(SecretKey key, byte @Nullable [] iv)
      throws GeneralSecurityException;

  /**
   * Creates a decrypting cipher that uses the key as the IV, as the pre-26.4 protocol does.
   *
   * @param key the AES key
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  default VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException {
    return forDecryption(key, null);
  }

  /**
   * Creates a decrypting cipher.
   *
   * @param key the AES key
   * @param iv the {@value #IV_LENGTH}-byte IV, or {@code null} to use the key as the IV
   * @return the cipher
   * @throws GeneralSecurityException if the cipher can't be initialized
   */
  VelocityCipher forDecryption(SecretKey key, byte @Nullable [] iv)
      throws GeneralSecurityException;
}
