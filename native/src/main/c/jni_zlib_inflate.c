#include <assert.h>
#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <zlib.h>
#include "jni_util.h"
#include "jni_zlib_common.h"

static jfieldID finishedID;
static jfieldID consumedID;

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_initIDs(JNIEnv *env, jclass cls)
{
    finishedID = (*env)->GetFieldID(env, cls, "finished", "Z");
    consumedID = (*env)->GetFieldID(env, cls, "consumed", "I");
}

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_init(JNIEnv *env,
    jobject obj)
{
    z_stream* stream = calloc(1, sizeof(z_stream));

    if (stream == 0) {
        // Out of memory!
        throwException(env, "java/lang/OutOfMemoryError", "zlib allocate stream");
        return 0;
    }

    int ret = inflateInit(stream);
    if (ret == Z_OK) {
        return (jlong) stream;
    } else {
        char *zlib_msg = stream->msg;
        free(stream);
        switch (ret) {
            case Z_MEM_ERROR:
                throwException(env, "java/lang/OutOfMemoryError", "zlib init");
                return 0;
            case Z_STREAM_ERROR:
                throwException(env, "java/lang/IllegalArgumentException", "stream clobbered?");
                return 0;
            default:
                throwException(env, "java/util/zip/DataFormatException", zlib_msg);
                return 0;
        }
    }
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_free(JNIEnv *env,
    jobject obj,
    jlong ctx)
{
    z_stream* stream = (z_stream*) ctx;
    check_zlib_free(env, stream, false);
}

JNIEXPORT int JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_process(JNIEnv *env,
    jobject obj,
    jlong ctx,
    jlong sourceAddress,
    jint sourceLength,
    jlong destinationAddress,
    jint destinationLength)
{
    z_stream* stream = (z_stream*) ctx;
    stream->next_in = (Bytef *) sourceAddress;
    stream->next_out = (Bytef *) destinationAddress;
    stream->avail_in = sourceLength;
    stream->avail_out = destinationLength;

    int res = inflate(stream, Z_PARTIAL_FLUSH);
    switch (res) {
        case Z_STREAM_END:
            // The stream has ended
            (*env)->SetBooleanField(env, obj, finishedID, JNI_TRUE);
            // fall-through
        case Z_OK:
            // Not yet completed, but progress has been made. Tell Java how many bytes we've processed.
            (*env)->SetIntField(env, obj, consumedID, sourceLength - stream->avail_in);
            return destinationLength - stream->avail_out;
        case Z_BUF_ERROR:
            // This is not fatal. Just say we need more data. Usually this applies to the next_out buffer,
            // which NativeVelocityCompressor will notice and will expand the buffer.
            return 0;
        default:
            throwException(env, "java/util/zip/DataFormatException", stream->msg);
            return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_reset(JNIEnv *env,
    jobject obj,
    jlong ctx)
{
    z_stream* stream = (z_stream*) ctx;
    int ret = inflateReset(stream);
    assert(ret == Z_OK);
}