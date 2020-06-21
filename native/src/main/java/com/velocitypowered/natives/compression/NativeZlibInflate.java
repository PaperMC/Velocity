package com.velocitypowered.natives.compression;

import java.util.zip.DataFormatException;

/**
 * Represents a native interface for zlib's inflate functions.
 */
class NativeZlibInflate {

  native long init();

  native long free(long ctx);

  native boolean process(long ctx, long sourceAddress, int sourceLength, long destinationAddress,
      int destinationLength) throws DataFormatException;
}
