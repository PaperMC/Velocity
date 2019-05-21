package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
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
  public void process(ByteBuf source, ByteBuf destination) throws ShortBufferException {
    ensureNotDisposed();
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
  public ByteBuf process(ChannelHandlerContext ctx, ByteBuf source) throws ShortBufferException {
    ensureNotDisposed();
    source.memoryAddress(); // sanity check

    int len = source.readableBytes();
    ByteBuf out = ctx.alloc().directBuffer(len);

    impl.process(this.ctx, source.memoryAddress() + source.readerIndex(), len,
        out.memoryAddress(), encrypt);
    source.skipBytes(len);
    out.writerIndex(len);
    return out;
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
  public boolean isNative() {
    return true;
  }
}
