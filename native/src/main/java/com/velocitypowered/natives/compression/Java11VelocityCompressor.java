/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.natives.compression;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.velocitypowered.natives.compression.CompressorUtils.ZLIB_BUFFER_SIZE;
import static com.velocitypowered.natives.compression.CompressorUtils.ensureMaxSize;

import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class Java11VelocityCompressor implements VelocityCompressor {

  public static final VelocityCompressorFactory FACTORY = Java11VelocityCompressor::new;

  // The use of MethodHandle is intentional. Velocity targets Java 8, and these methods don't exist
  // in Java 8. This was also the most performant solution I could find, only slightly slower than a
  // direct method call without long warmup times, requiring bytecode generation through ASM, or
  // other stuff.
  private static final MethodHandle DEFLATE_SET_INPUT;
  private static final MethodHandle INFLATE_SET_INPUT;
  private static final MethodHandle DEFLATE_CALL;
  private static final MethodHandle INFLATE_CALL;

  static {
    try {
      DEFLATE_SET_INPUT = MethodHandles.lookup().findVirtual(Deflater.class, "setInput",
          MethodType.methodType(void.class, ByteBuffer.class));
      INFLATE_SET_INPUT = MethodHandles.lookup().findVirtual(Inflater.class, "setInput",
          MethodType.methodType(void.class, ByteBuffer.class));

      DEFLATE_CALL = MethodHandles.lookup().findVirtual(Deflater.class, "deflate",
          MethodType.methodType(int.class, ByteBuffer.class));
      INFLATE_CALL = MethodHandles.lookup().findVirtual(Inflater.class, "inflate",
          MethodType.methodType(int.class, ByteBuffer.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError("Can't use Java 11 compressor on your version of Java");
    }
  }

  private final Deflater deflater;
  private final Inflater inflater;
  private boolean disposed = false;

  private Java11VelocityCompressor(int level) {
    this.deflater = new Deflater(level);
    this.inflater = new Inflater();
  }

  @Override
  public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize)
      throws DataFormatException {
    ensureNotDisposed();

    // We (probably) can't nicely deal with >=1 buffer nicely, so let's scream loudly.
    checkArgument(source.nioBufferCount() == 1, "source has multiple backing buffers");
    checkArgument(destination.nioBufferCount() == 1, "destination has multiple backing buffers");

    try {
      final int origIdx = source.readerIndex();
      INFLATE_SET_INPUT.invokeExact(inflater, source.nioBuffer());

      while (!inflater.finished() && inflater.getBytesWritten() < uncompressedSize) {
        if (!destination.isWritable()) {
          ensureMaxSize(destination, uncompressedSize);
          destination.ensureWritable(ZLIB_BUFFER_SIZE);
        }

        ByteBuffer destNioBuf = destination.nioBuffer(destination.writerIndex(),
            destination.writableBytes());
        int produced = (int) INFLATE_CALL.invokeExact(inflater, destNioBuf);
        destination.writerIndex(destination.writerIndex() + produced);
      }

      if (!inflater.finished()) {
        throw new DataFormatException("Received a deflate stream that was too large, wanted "
          + uncompressedSize);
      }
      source.readerIndex(origIdx + inflater.getTotalIn());
    } catch (Throwable e) {
      if (e instanceof DataFormatException) {
        throw (DataFormatException) e;
      }
      throw new RuntimeException(e);
    } finally {
      inflater.reset();
    }
  }

  @Override
  public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
    ensureNotDisposed();

    // We (probably) can't nicely deal with >=1 buffer nicely, so let's scream loudly.
    checkArgument(source.nioBufferCount() == 1, "source has multiple backing buffers");
    checkArgument(destination.nioBufferCount() == 1, "destination has multiple backing buffers");

    try {
      final int origIdx = source.readerIndex();
      DEFLATE_SET_INPUT.invokeExact(deflater, source.nioBuffer());
      deflater.finish();

      while (!deflater.finished()) {
        if (!destination.isWritable()) {
          destination.ensureWritable(ZLIB_BUFFER_SIZE);
        }

        ByteBuffer destNioBuf = destination.nioBuffer(destination.writerIndex(),
            destination.writableBytes());
        int produced = (int) DEFLATE_CALL.invokeExact(deflater, destNioBuf);
        destination.writerIndex(destination.writerIndex() + produced);
      }

      source.readerIndex(origIdx + deflater.getTotalIn());
      deflater.reset();
    } catch (Throwable e) {
      if (e instanceof DataFormatException) {
        throw (DataFormatException) e;
      }
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {
    disposed = true;
    deflater.end();
    inflater.end();
  }

  private void ensureNotDisposed() {
    checkState(!disposed, "Object already disposed");
  }

  @Override
  public BufferPreference preferredBufferType() {
    return BufferPreference.DIRECT_PREFERRED;
  }
}
