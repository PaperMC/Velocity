#include <assert.h>
#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <libdeflate.h>
#include "jni_util.h"

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_init(JNIEnv *env,
    jclass clazz)
{
    struct libdeflate_decompressor *decompress = libdeflate_alloc_decompressor();
    if (decompress == NULL) {
        // Out of memory!
        throwException(env, "java/lang/OutOfMemoryError", "libdeflate allocate decompressor");
        return 0;
    }

    return (jlong) decompress;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_free(JNIEnv *env,
    jclass clazz,
    jlong ctx)
{
    libdeflate_free_decompressor((struct libdeflate_decompressor *) ctx);
}

JNIEXPORT jboolean JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_process(JNIEnv *env,
    jclass clazz,
    jlong ctx,
    jlong sourceAddress,
    jint sourceLength,
    jlong destinationAddress,
    jint destinationLength,
    jlong maximumSize)
{
    struct libdeflate_decompressor *decompress = (struct libdeflate_decompressor *) ctx;
    enum libdeflate_result result = libdeflate_zlib_decompress(decompress, (void *) sourceAddress,
        sourceLength, (void *) destinationAddress, destinationLength, NULL);

    switch (result) {
        case LIBDEFLATE_SUCCESS:
            // We are happy
            return JNI_TRUE;
        case LIBDEFLATE_BAD_DATA:
            throwException(env, "java/util/zip/DataFormatException", "inflate data is bad");
            return JNI_FALSE;
        case LIBDEFLATE_SHORT_OUTPUT:
        case LIBDEFLATE_INSUFFICIENT_SPACE:
            // These cases are the same for us. We expect the full uncompressed size to be known.
            throwException(env, "java/util/zip/DataFormatException", "uncompressed size is inaccurate");
            return JNI_FALSE;
        default:
            // Unhandled case
            throwException(env, "java/util/zip/DataFormatException", "unknown libdeflate return code");
            return JNI_FALSE;
    }
}