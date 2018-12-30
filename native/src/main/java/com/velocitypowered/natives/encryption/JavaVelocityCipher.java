package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;

public class JavaVelocityCipher implements VelocityCipher {

  public static final VelocityCipherFactory FACTORY = new VelocityCipherFactory() {
    @Override
    public VelocityCipher forEncryption(SecretKey key) throws GeneralSecurityException {
      return new JavaVelocityCipher(true, key);
    }

    @Override
    public VelocityCipher forDecryption(SecretKey key) throws GeneralSecurityException {
      return new JavaVelocityCipher(false, key);
    }
  };
  private static final int INITIAL_BUFFER_SIZE = 1024 * 8;
  private static final ThreadLocal<byte[]> inBufLocal = ThreadLocal.withInitial(
      () -> new byte[INITIAL_BUFFER_SIZE]);

  private final Cipher cipher;
  private boolean disposed = false;

  private JavaVelocityCipher(boolean encrypt, SecretKey key) throws GeneralSecurityException {
    this.cipher = Cipher.getInstance("AES/CFB8/NoPadding");
    this.cipher.init(encrypt ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key,
        new IvParameterSpec(key.getEncoded()));
  }

  @Override
  public void process(ByteBuf source, ByteBuf destination) throws ShortBufferException {
    ensureNotDisposed();

    int inBytes = source.readableBytes();
    ByteBuf asHeapBuf = asHeapBuf(source);

    int outputSize = cipher.getOutputSize(inBytes);
    if (!destination.hasArray()) {
      byte[] outBuf = new byte[outputSize];
      cipher.update(asHeapBuf.array(), asHeapBuf.arrayOffset(), inBytes, outBuf);
      destination.writeBytes(outBuf);
    } else {
      // If the destination we write to is an array, we can use the backing array directly.
      destination.ensureWritable(outputSize);
      int produced = cipher.update(asHeapBuf.array(), asHeapBuf.arrayOffset(), inBytes,
          destination.array(), destination.arrayOffset());
      destination.writerIndex(destination.writerIndex() + produced);
    }
  }

  @Override
  public ByteBuf process(ChannelHandlerContext ctx, ByteBuf source) throws ShortBufferException {
    ensureNotDisposed();

    int inBytes = source.readableBytes();
    ByteBuf asHeapBuf = asHeapBuf(source);

    ByteBuf out = ctx.alloc().heapBuffer(cipher.getOutputSize(inBytes));
    out.writerIndex(cipher.update(asHeapBuf.array(), asHeapBuf.arrayOffset(), inBytes, out.array(),
        out.arrayOffset()));
    return out;
  }

  private static ByteBuf asHeapBuf(ByteBuf source) {
    if (source.hasArray()) {
      // If this byte buffer is backed by an array, we can just use this buffer directly.
      return source;
    }

    int inBytes = source.readableBytes();
    byte[] inBuf = inBufLocal.get();
    if (inBuf.length <= inBytes) {
      inBuf = new byte[inBytes];
      inBufLocal.set(inBuf);
    }
    source.readBytes(inBuf, 0, inBytes);
    return Unpooled.wrappedBuffer(inBuf, 0, inBytes);
  }

  @Override
  public void dispose() {
    disposed = true;
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public boolean isNative() {
    return false;
  }
}
