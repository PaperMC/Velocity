package com.velocitypowered.natives.encryption;

import io.netty.buffer.ByteBuf;
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

  private NativeVelocityCipher(boolean encrypt, SecretKey key) {
    this.encrypt = encrypt;
    this.ctx = impl.init(key.getEncoded());
  }

  @Override
  public void process(ByteBuf source, ByteBuf destination) throws ShortBufferException {
    source.memoryAddress();
    destination.memoryAddress();

    // The exact amount we read in is also the amount we write out.
    int len = source.readableBytes();
    destination.ensureWritable(len);

    impl.process(ctx, source.memoryAddress() + source.readerIndex(), len,
        destination.memoryAddress() + destination.writerIndex(), encrypt);

    source.skipBytes(len);
    destination.writerIndex(destination.writerIndex() + len);
  }

  @Override
  public void dispose() {
    if (!disposed) {
      impl.free(ctx);
    }
    disposed = true;
  }
}
