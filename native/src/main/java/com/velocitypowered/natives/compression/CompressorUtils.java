package com.velocitypowered.natives.compression;

import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

class CompressorUtils {
  /**
   * The default preferred output buffer size for zlib.
   */
  static final int ZLIB_BUFFER_SIZE = 8192;

  /**
   * Ensures that the buffer does not go over {@code max}.
   * @param buf the buffer for check
   * @param max the maximum size for the buffer
   * @throws DataFormatException if the buffer becomes too bug
   */
  static void ensureMaxSize(ByteBuf buf, int max) throws DataFormatException {
    int len = buf.readableBytes();
    if (len > max) {
      throw new DataFormatException("Got too much data (" + len + " > " + max + ")");
    }
  }

  private CompressorUtils() {
    throw new AssertionError();
  }
}
