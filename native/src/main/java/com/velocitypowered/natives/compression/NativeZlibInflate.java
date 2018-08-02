package com.velocitypowered.natives.compression;

/**
 * Represents a native interface for zlib's inflate functions.
 */
public class NativeZlibInflate {
    boolean finished;
    int consumed;

    native long init();

    native long free(long ctx);

    native int process(long ctx, long sourceAddress, int sourceLength, long destinationAddress, int destinationLength);

    native void reset(long ctx);

    static {
        initIDs();
    }

    static native void initIDs();
}
