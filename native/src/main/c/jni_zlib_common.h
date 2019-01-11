#include <jni.h>
#include <stdbool.h>
#include <zlib.h>

void JNICALL
check_zlib_free(JNIEnv *env, z_stream *stream, bool deflate);