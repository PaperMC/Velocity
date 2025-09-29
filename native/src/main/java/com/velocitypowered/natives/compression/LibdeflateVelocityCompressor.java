/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import com.google.common.base.Preconditions;
import com.velocitypowered.natives.util.BufferPreference;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

/**
 * Implements deflate compression using the {@code libdeflate} native C library.
 */
public class LibdeflateVelocityCompressor implements VelocityCompressor {

  public static final VelocityCompressorFactory FACTORY = LibdeflateVelocityCompressor::new;

  private final long inflateCtx;
  private final long deflateCtx;
  private boolean disposed = false;

  private LibdeflateVelocityCompressor(int level) {
    int correctedLevel = level == -1 ? 6 : level;
    if (correctedLevel > 12 || correctedLevel < 1) {
      throw new IllegalArgumentException("Invalid compression level " + level);
    }

    this.inflateCtx = NativeZlibInflate.init();
    this.deflateCtx = NativeZlibDeflate.init(correctedLevel);
  }

  @Override
  public void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize)
      throws DataFormatException {
    ensureNotDisposed();

    // libdeflate recommends we work with a known uncompressed size - so we work strictly within
    // those parameters. If the uncompressed size doesn't match the compressed size, then we will
    // throw an exception from native code.
    destination.ensureWritable(uncompressedSize);

    long sourceAddress = source.memoryAddress() + source.readerIndex();
    long destinationAddress = destination.memoryAddress() + destination.writerIndex();

    NativeZlibInflate.process(inflateCtx, sourceAddress, source.readableBytes(), destinationAddress,
        uncompressedSize);
    destination.writerIndex(destination.writerIndex() + uncompressedSize);
  }

  @Override
  public void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException {
    ensureNotDisposed();

    while (true) {
      long sourceAddress = source.memoryAddress() + source.readerIndex();
      long destinationAddress = destination.memoryAddress() + destination.writerIndex();

      int produced = NativeZlibDeflate.process(deflateCtx, sourceAddress, source.readableBytes(),
          destinationAddress, destination.writableBytes());
      if (produced > 0) {
        destination.writerIndex(destination.writerIndex() + produced);
        break;
      } else if (produced == 0) {
        // Insufficient room - enlarge the buffer.
        destination.capacity(destination.capacity() * 2);
      } else {
        throw new DataFormatException("libdeflate returned unknown code " + produced);
      }
    }
  }

  private void ensureNotDisposed() {
    Preconditions.checkState(!disposed, "Object already disposed");
  }

  @Override
  public void close() {
    if (!disposed) {
      NativeZlibInflate.free(inflateCtx);
      NativeZlibDeflate.free(deflateCtx);
    }
    disposed = true;
  }

  @Override
  public BufferPreference preferredBufferType() {
    return BufferPreference.DIRECT_REQUIRED;
  }
}
