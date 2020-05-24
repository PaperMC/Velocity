package com.velocitypowered.natives.compression;

/**
 * Represents a native interface for zlib's deflate functions.
 */
class NativeZlibDeflate {

  native long init(int level);

  native long free(long ctx);

  native int process(long ctx, long sourceAddress, int sourceLength, long destinationAddress,
      int destinationLength);
}
