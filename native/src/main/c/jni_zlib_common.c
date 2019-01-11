#include <jni.h>
#include <stdbool.h>
#include <stdlib.h>
#include <zlib.h>
#include "jni_util.h"

void JNICALL
check_zlib_free(JNIEnv *env, z_stream *stream, bool deflate)
{
    int ret = deflate ? deflateEnd(stream) : inflateEnd(stream);
    char *msg = stream->msg;
    free((void*) stream);

    switch (ret) {
        case Z_OK:
            break;
        case Z_STREAM_ERROR:
            if (msg == NULL) {
                msg = "stream state inconsistent";
            }
            // fall-through
        case Z_DATA_ERROR:
            if (msg == NULL) {
                msg = "data was discarded";
            }
            throwException(env, "java/lang/IllegalArgumentException", msg);
            break;
    }
}