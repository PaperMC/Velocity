#include <assert.h>
#include <jni.h>
#include <stdlib.h>
#include <zlib.h>
#include "jni_util.h"

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

    char *msg;
    int ret = inflateInit(stream);

    switch (ret) {
        case Z_OK:
            return (jlong) stream;
        case Z_MEM_ERROR:
            free(stream);
            throwException(env, "java/lang/OutOfMemoryError", "zlib init");
            return 0;
        case Z_STREAM_ERROR:
            free(stream);
            throwException(env, "java/lang/IllegalArgumentException", "stream clobbered?");
            return 0;
        default:
            msg = stream->msg;
            free(stream);
            throwException(env, "java/util/zip/DataFormatException", msg);
            return 0;
    }
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_compression_NativeZlibInflate_free(JNIEnv *env,
    jobject obj,
    jlong ctx)
{
    z_stream* stream = (z_stream*) ctx;
    int ret = inflateEnd(stream);
    char *msg = stream->msg;
    free((void*) ctx);

    switch (ret) {
        case Z_OK:
            break;
        case Z_STREAM_ERROR:
            if (msg == NULL) {
                msg = "stream state inconsistent";
            }
        case Z_DATA_ERROR:
            if (msg == NULL) {
                msg = "data was discarded";
            }
            throwException(env, "java/lang/IllegalArgumentException", msg);
            break;
    }
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