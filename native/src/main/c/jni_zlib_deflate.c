#include <assert.h>
#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <libdeflate.h>
#include "jni_util.h"

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibDeflate_init(JNIEnv *env,
    jclass clazz,
    jint level)
{
    struct libdeflate_compressor *compressor = libdeflate_alloc_compressor(level);
    if (compressor == NULL) {
        // Out of memory!
        throwException(env, "java/lang/OutOfMemoryError", "libdeflate allocate compressor");
        return 0;
    }
    return (jlong) compressor;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibDeflate_free(JNIEnv *env,
    jclass clazz,
    jlong ctx)
{
    libdeflate_free_compressor((struct libdeflate_compressor *) ctx);
}

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibDeflate_process(JNIEnv *env,
    jclass clazz,
    jlong ctx,
    jlong sourceAddress,
    jint sourceLength,
    jlong destinationAddress,
    jint destinationLength)
{
    struct libdeflate_compressor *compressor = (struct libdeflate_compressor *) ctx;
    size_t produced = libdeflate_zlib_compress(compressor, (void *) sourceAddress, sourceLength,
        (void *) destinationAddress, destinationLength);
    return (jlong) produced;
}