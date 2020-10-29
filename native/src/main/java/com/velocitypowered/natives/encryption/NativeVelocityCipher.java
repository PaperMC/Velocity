package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;

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
