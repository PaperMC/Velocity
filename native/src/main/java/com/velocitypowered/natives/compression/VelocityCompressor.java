package com.velocitypowered.natives.compression;

import com.velocitypowered.natives.Disposable;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

/**
 * Provides an interface to inflate and deflate {@link ByteBuf}s using zlib.
 */
public interface VelocityCompressor extends Disposable {

  /**
   * The default preferred output buffer size for zlib.
   */
  int ZLIB_BUFFER_SIZE = 8192;

  void inflate(ByteBuf source, ByteBuf destination) throws DataFormatException;

  void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;
}
