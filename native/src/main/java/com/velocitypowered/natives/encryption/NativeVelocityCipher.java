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

package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;

/**
 * Implements AES-CFB8 encryption/decryption using a native library.
 */
public class NativeVelocityCipher implements VelocityCipher {

  public static final VelocityCipherFactory FACTORY = new VelocityCipherFactory() {
    @Override
    public VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException {
      return new NativeVelocityCipher(true, key);
    }

    @Override
    public VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException {
      return new NativeVelocityCipher(false, key);
    }
  };
  private final long ctx;
  private boolean disposed = false;

  private NativeVelocityCipher(boolean encrypt, SecretKey key) throws GeneralSecurityException {
    this.ctx = OpenSslCipherImpl.init(key.getEncoded(), encrypt);
  }

  @Override
  public void process(ByteBuf source) {
    ensureNotDisposed();

    long base = source.memoryAddress() + source.readerIndex();
    int len = source.readableBytes();

    OpenSslCipherImpl.process(ctx, base, len, base);
  }

  @Override
  public void close() {
    if (!disposed) {
      OpenSslCipherImpl.free(ctx);
    }
    disposed = true;
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public BufferPreference preferredBufferType() {
    return BufferPreference.DIRECT_REQUIRED;
  }
}
