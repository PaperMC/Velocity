package com.velocitypowered.natives.compression;

import static com.velocitypowered.natives.compression.CompressorUtils.ZLIB_BUFFER_SIZE;
import static com.velocitypowered.natives.compression.CompressorUtils.ensureMaxSize;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

public class NativeVelocityCompressor implements VelocityCompressor {

  public static final VelocityCompressorFactory FACTORY = NativeVelocityCompressor::new;

  private final NativeZlibInflate inflate = new NativeZlibInflate();
  private final long inflateCtx;
  private final NativeZlibDeflate deflate = new NativeZlibDeflate();
  private final long deflateCtx;
  private boolean disposed = false;

  private NativeVelocityCompressor(int level) {
    this.inflateCtx = inflate.init();
    this.deflateCtx = deflate.init(level);
  }

  @Override
  public void inflate(ByteBuf source, ByteBuf destination, int max) throws DataFormatException {
    ensureNotDisposed();
    source.memoryAddress();
    destination.memoryAddress();

    while (!inflate.finished && source.isReadable()) {
      if (!destination.isWritable()) {
        ensureMaxSize(destination, max);
        destination.ensureWritable(ZLIB_BUFFER_SIZE);
      }
      int produced = inflate.process(inflateCtx, source.memoryAddress() + source.readerIndex(),
          source.readableBytes(), destination.memoryAddress() + destination.writerIndex(),
          destination.writableBytes());
      source.readerIndex(source.readerIndex() + inflate.consumed);
      destination.writerIndex(destination.writerIndex() + produced);
    }

    inflate.reset(inflateCtx);
    inflate.consumed = 0;
    inflate.finished = false;
  }

  @Override
  public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
    ensureNotDisposed();
    source.memoryAddress();
    destination.memoryAddress();

    while (!deflate.finished) {
      if (!destination.isWritable()) {
        destination.ensureWritable(ZLIB_BUFFER_SIZE);
      }
      int produced = deflate.process(deflateCtx, source.memoryAddress() + source.readerIndex(),
          source.readableBytes(),
          destination.memoryAddress() + destination.writerIndex(), destination.writableBytes(),
          true);
      source.readerIndex(source.readerIndex() + deflate.consumed);
      destination.writerIndex(destination.writerIndex() + produced);
    }

    deflate.reset(deflateCtx);
    deflate.consumed = 0;
    deflate.finished = false;
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public void dispose() {
    if (!disposed) {
      inflate.free(inflateCtx);
      deflate.free(deflateCtx);
    }
    disposed = true;
  }

  @Override
  public boolean isNative() {
    return true;
  }
}
