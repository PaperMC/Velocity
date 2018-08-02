#include <jni.h>

JNIEXPORT void JNICALL
throwException(JNIEnv *env, const char *type, const char *msg);