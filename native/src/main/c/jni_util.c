#include <jni.h>
#include "jni_util.h"

void JNICALL
throwException(JNIEnv *env, const char *type, const char *msg)
{
    jclass klazz = (*env)->FindClass(env, type);

    if (klazz != 0) {
        (*env)->ThrowNew(env, klazz, msg);
    }
}