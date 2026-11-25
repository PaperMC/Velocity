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

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Implements AES-CFB8 encryption/decryption using {@link Cipher}.
 */
public class JavaVelocityCipher implements VelocityCipher {

  public static final VelocityCipherFactory FACTORY = new VelocityCipherFactory() {
    @Override
    public VelocityCipher forEncryption(SecretKey key, byte @Nullable [] iv)
        throws GeneralSecurityException {
      return new JavaVelocityCipher(true, key, iv);
    }

    @Override
    public VelocityCipher forDecryption(SecretKey key, byte @Nullable [] iv)
        throws GeneralSecurityException {
      return new JavaVelocityCipher(false, key, iv);
    }
  };

  private final Cipher cipher;
  private boolean disposed = false;

  private JavaVelocityCipher(boolean encrypt, SecretKey key, byte @Nullable [] iv)
      throws GeneralSecurityException {
    final byte[] effectiveIv;
    if (iv == null) {
      // Before 26.4, the protocol used the shared secret as both the key and the IV, in both
      // directions. Reusing the key as the IV defeats the entire point of an IV - we might as well
      // just initialize it to all zeroes - and because both directions use the same key and IV,
      // both streams start out in the same cipher state.
      //
      // 26.4 replaced this with a proper key exchange that derives a separate key and IV for each
      // direction. Not passing an IV (insecure!) is only for older protocols.
      effectiveIv = key.getEncoded();
    } else if (iv.length == VelocityCipherFactory.IV_LENGTH) {
      effectiveIv = iv;
    } else {
      throw new IllegalArgumentException("IV not " + VelocityCipherFactory.IV_LENGTH + " bytes");
    }
    this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
    this.cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key,
        new IvParameterSpec(effectiveIv));
  }

  @Override
  public void process(ByteBuf source) {
    ensureNotDisposed();
    Preconditions.checkArgument(source.hasArray(), "No source array");

    int inBytes = source.readableBytes();
    int baseOffset = source.arrayOffset() + source.readerIndex();

    try {
      cipher.update(source.array(), baseOffset, inBytes, source.array(), baseOffset);
    } catch (ShortBufferException ex) {
      /* This _really_ shouldn't happen - AES CFB8 will work in place.
         If you run into this, that means that for whatever reason the Java Runtime has determined
         that the output buffer needs more bytes than the input buffer. When we are working with
         AES-CFB8, the output size is equal to the input size. See the problem? */
      throw new AssertionError("Cipher update did not operate in place and requested a larger "
              + "buffer than the source buffer");
    }
  }

  @Override
  public void close() {
    disposed = true;
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public BufferPreference preferredBufferType() {
    return BufferPreference.HEAP_REQUIRED;
  }
}
