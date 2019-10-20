package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import java.security.GeneralSecurityException;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;

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
  private static final MbedtlsAesImpl impl = new MbedtlsAesImpl();

  private final long ctx;
  private final boolean encrypt;
  private boolean disposed = false;

  private NativeVelocityCipher(boolean encrypt, SecretKey key) throws GeneralSecurityException {
    this.encrypt = encrypt;
    this.ctx = impl.init(key.getEncoded());
  }

  @Override
  public void process(ByteBuf source) {
    ensureNotDisposed();
    source.memoryAddress();

    long base = source.memoryAddress() + source.readerIndex();
    int len = source.readableBytes();

    impl.process(ctx, base, len, base, encrypt);
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
