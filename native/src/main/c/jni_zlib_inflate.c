#include <jni.h>
#include <stdlib.h>
#include <igzip_lib.h>
#include "jni_util.h"

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_init(JNIEnv *env,
    jclass clazz)
{
    struct inflate_state *decompress = malloc(sizeof(struct inflate_state));
    if (decompress == NULL) {
        // Out of memory!
        throwException(env, "java/lang/OutOfMemoryError", "isa-l inflate state allocation");
        return 0;
    }

    decompress->crc_flag = IGZIP_ZLIB;

    return (jlong) decompress;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_free(JNIEnv *env,
    jclass clazz,
    jlong ctx)
{
    free((struct inflate_state *) ctx);
}

JNIEXPORT jboolean JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_process(JNIEnv *env,
    jclass clazz,
    jlong ctx,
    jlong sourceAddress,
    jint sourceLength,
    jlong destinationAddress,
    jint destinationLength)
{
    struct inflate_state *decompress = (struct inflate_state *) ctx;
    decompress->next_in = (uint8_t *) sourceAddress;
    decompress->avail_in = sourceLength;
    decompress->next_out = (uint8_t *) destinationAddress;
    decompress->avail_out = destinationLength;

    int result = isal_inflate_stateless(decompress);
    isal_inflate_reset(decompress);

    if (result == ISAL_DECOMP_OK) {
        return JNI_TRUE;
    } else {
        throwException(env, "java/util/zip/DataFormatException", "inflate data is bad");
        return JNI_FALSE;
    }
}