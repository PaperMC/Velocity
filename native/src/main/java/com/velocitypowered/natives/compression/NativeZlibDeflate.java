package com.velocitypowered.natives.compression;

/**
 * Represents a native interface for zlib's deflate functions.
 */
class NativeZlibDeflate {

  static native long init(int level);

  static native long free(long ctx);

  static native int process(long ctx, long sourceAddress, int sourceLength, long destinationAddress,
      int destinationLength);
}
