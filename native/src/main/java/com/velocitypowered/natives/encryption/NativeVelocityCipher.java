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
  private static final OpenSslCipherImpl impl = new OpenSslCipherImpl();

  private final long ctx;
  private boolean disposed = false;

  private NativeVelocityCipher(boolean encrypt, SecretKey key) throws GeneralSecurityException {
    this.ctx = impl.init(key.getEncoded(), encrypt);
  }

  @Override
  public void process(ByteBuf source) {
    ensureNotDisposed();

    long base = source.memoryAddress() + source.readerIndex();
    int len = source.readableBytes();

    impl.process(ctx, base, len, base);
  }

  @Override
  public void dispose() {
    if (!disposed) {
      impl.free(ctx);
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
