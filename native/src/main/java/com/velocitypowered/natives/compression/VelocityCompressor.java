package com.velocitypowered.natives.compression;

import com.velocitypowered.natives.Disposable;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

/**
 * Provides an interface to inflate and deflate {@link ByteBuf}s using zlib.
 */
public interface VelocityCompressor extends Disposable {
  void inflate(ByteBuf source, ByteBuf destination) throws DataFormatException;

  void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;
}
