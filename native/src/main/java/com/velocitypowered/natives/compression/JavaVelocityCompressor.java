package com.velocitypowered.natives.compression;

import static com.velocitypowered.natives.util.NativeConstants.ZLIB_BUFFER_SIZE;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class JavaVelocityCompressor implements VelocityCompressor {

  public static final VelocityCompressorFactory FACTORY = JavaVelocityCompressor::new;

  private final Deflater deflater;
  private final Inflater inflater;
  private final byte[] buf;
  private boolean disposed = false;

  private JavaVelocityCompressor(int level) {
    this.deflater = new Deflater(level);
    this.inflater = new Inflater();
    this.buf = new byte[ZLIB_BUFFER_SIZE];
  }

  @Override
  public void inflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
    ensureNotDisposed();

    byte[] inData = new byte[source.readableBytes()];
    source.readBytes(inData);
    inflater.setInput(inData);
    while (!inflater.finished()) {
      int read = inflater.inflate(buf);
      destination.writeBytes(buf, 0, read);
    }
    inflater.reset();
  }

  @Override
  public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
    ensureNotDisposed();

    byte[] inData = new byte[source.readableBytes()];
    source.readBytes(inData);
    deflater.setInput(inData);
    deflater.finish();
    while (!deflater.finished()) {
      int bytes = deflater.deflate(buf);
      destination.writeBytes(buf, 0, bytes);
    }
    deflater.reset();
  }

  @Override
  public void dispose() {
    disposed = true;
    deflater.end();
    inflater.end();
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public boolean isNative() {
    return false;
  }
}
