package com.velocitypowered.natives.encryption;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
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
    byte[] asBytes = ByteBufUtil.getBytes(source);

    int outputSize = cipher.getOutputSize(inBytes);
    byte[] outBuf = new byte[outputSize];
    cipher.update(asBytes, 0, inBytes, outBuf);
    destination.writeBytes(outBuf);
  }

  @Override
  public ByteBuf process(ChannelHandlerContext ctx, ByteBuf source) throws ShortBufferException {
    ensureNotDisposed();

    int inBytes = source.readableBytes();
    ByteBuf asHeapBuf = toHeap(source);
    ByteBuf out = ctx.alloc().heapBuffer(cipher.getOutputSize(inBytes));
    try {
      out.writerIndex(
          cipher.update(asHeapBuf.array(), asHeapBuf.arrayOffset() + asHeapBuf.readerIndex(),
              inBytes, out.array(), out.arrayOffset() + out.writerIndex()));
      return out;
    } catch (ShortBufferException e) {
      out.release();
      throw e;
    } finally {
      asHeapBuf.release();
    }
  }

  private static ByteBuf toHeap(ByteBuf src) {
    if (src.hasArray()) {
      return src.retain();
    }

    // Copy into a temporary heap buffer. We could use a local buffer, but Netty pools all buffers,
    // so we'd lose more than we gain.
    ByteBuf asHeapBuf = src.alloc().heapBuffer(src.readableBytes());
    asHeapBuf.writeBytes(src);
    return asHeapBuf;
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
