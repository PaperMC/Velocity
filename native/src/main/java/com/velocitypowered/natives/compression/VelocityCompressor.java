package com.velocitypowered.natives.compression;

import com.velocitypowered.natives.Disposable;
import com.velocitypowered.natives.Native;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

/**
 * Provides an interface to inflate and deflate {@link ByteBuf}s using zlib or a compatible
 * implementation.
 */
public interface VelocityCompressor extends Disposable, Native {
  void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize)
      throws DataFormatException;

  void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;
}
