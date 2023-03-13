#include <jni.h>
#include <stdlib.h>
#include <igzip_lib.h>
#include "jni_util.h"

const static uint32_t levelBufferSizes[] = {
    ISAL_DEF_LVL0_DEFAULT,
    ISAL_DEF_LVL1_DEFAULT,
    ISAL_DEF_LVL2_DEFAULT,
    ISAL_DEF_LVL3_DEFAULT
};

const static uint32_t levelMappings[] = {
    0, 0, 1, 1, 1, 1, 2, 3, 3, 3
};

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibDeflate_init(JNIEnv *env,
    jclass clazz,
    jint level)
{
    struct isal_zstream *compressor = malloc(sizeof(struct isal_zstream));
    if (compressor == NULL) {
        // Out of memory!
        throwException(env, "java/lang/OutOfMemoryError", "isa-l zstream allocation");
        return 0;
    }

    isal_deflate_init(compressor);
    compressor->level = levelMappings[level];
    compressor->level_buf_size = levelBufferSizes[compressor->level];
    compressor->level_buf = malloc(compressor->level_buf_size);
    compressor->gzip_flag = IGZIP_ZLIB_NO_HDR; // IGZIP_ZLIB doesn't write compression level correctly
    compressor->flush = NO_FLUSH;

    if (compressor->level_buf_size != 0 && compressor->level_buf == NULL) {
        throwException(env, "java/lang/OutOfMemory", "level buffer allocation");
        return 0;
    }

    return (jlong) compressor;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibDeflate_free(JNIEnv *env,
    jclass clazz,
    jlong ctx)
{
    struct isal_zstream *compressor = (struct isal_zstream *) ctx;

    if (compressor->level_buf != NULL) {
      free(compressor->level_buf);
    }

    free(compressor);
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
    struct isal_zstream *compressor = (struct isal_zstream *) ctx;
    compressor->next_in = (uint8_t *) sourceAddress;
    compressor->avail_in = sourceLength;
    compressor->next_out = (uint8_t *) destinationAddress;
    compressor->avail_out = destinationLength;

    struct isal_zlib_header header;
    header.info = 7;
    header.level = compressor->level;
    header.dict_flag = 0;

    if (isal_write_zlib_header(compressor, &header)) {
        isal_deflate_reset(compressor);
        return 0;
    }

    int result = isal_deflate_stateless(compressor);
    jlong produced = compressor->total_out;
    isal_deflate_reset(compressor);

    if (result == STATELESS_OVERFLOW) {
        return 0;
    } else if (result != COMP_OK) {
        throwException(env, "java/lang/IllegalStateException", "couldn't deflate data");
        return 0;
    }

    return (jlong) produced;
}