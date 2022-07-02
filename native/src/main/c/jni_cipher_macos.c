#include <CommonCrypto/CommonCryptor.h>
#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include "jni_util.h"

typedef unsigned char byte;

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_encryption_OpenSslCipherImpl_init(JNIEnv *env,
    jclass clazz,
    jbyteArray key,
    jboolean encrypt)
{
    jsize keyLen = (*env)->GetArrayLength(env, key);
    if (keyLen != 16) {
        throwException(env, "java/lang/IllegalArgumentException", "cipher not 16 bytes");
        return 0;
    }

    // Since we know the array size is always bounded, we can just use Get<Primitive>ArrayRegion
    // and save ourselves some error-checking headaches.
    jbyte keyBytes[16];
    (*env)->GetByteArrayRegion(env, key, 0, keyLen, (jbyte*) keyBytes);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    CCCryptorRef cryptor = NULL;
    CCCryptorStatus result = CCCryptorCreateWithMode(encrypt ? kCCEncrypt : kCCDecrypt,
        kCCModeCFB8,
        kCCAlgorithmAES128,
        ccNoPadding,
        keyBytes,
        keyBytes,
        16,
        NULL,
        0,
        0,
        0,
        &cryptor);
    if (result != kCCSuccess) {
        throwException(env, "java/security/GeneralSecurityException", "openssl initialize cipher");
        return 0;
    }
    return (jlong) cryptor;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_encryption_OpenSslCipherImpl_free(JNIEnv *env,
    jclass clazz,
    jlong ptr)
{
    CCCryptorRelease((CCCryptorRef) ptr);
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_encryption_OpenSslCipherImpl_process(JNIEnv *env,
    jclass clazz,
    jlong ptr,
    jlong source,
    jint len,
    jlong dest)
{
    CCCryptorUpdate((CCCryptorRef) ptr, (byte*) source, len, (byte*) dest, len, NULL);
}