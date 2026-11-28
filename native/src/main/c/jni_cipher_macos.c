// Needed for memset_s, which (unlike memset) the compiler may not optimize away.
#define __STDC_WANT_LIB_EXT1__ 1
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
    jbyteArray iv,
    jboolean encrypt)
{
    if (key == NULL) {
        throwException(env, "java/lang/NullPointerException", "key");
        return 0;
    }
    if (iv == NULL) {
        throwException(env, "java/lang/NullPointerException", "iv");
        return 0;
    }

    jsize keyLen = (*env)->GetArrayLength(env, key);
    if (keyLen != 16) {
        throwException(env, "java/lang/IllegalArgumentException", "key not 16 bytes");
        return 0;
    }

    jsize ivLen = (*env)->GetArrayLength(env, iv);
    if (ivLen != 16) {
        throwException(env, "java/lang/IllegalArgumentException", "IV not 16 bytes");
        return 0;
    }

    // Since we know the array size is always bounded, we can just use Get<Primitive>ArrayRegion
    // and save ourselves some error-checking headaches. Both buffers hold key material, so they
    // are wiped before returning on every path.
    jbyte keyBytes[16];
    jbyte ivBytes[16];
    jlong ret = 0;
    CCCryptorRef cryptor = NULL;
    CCCryptorStatus result;

    (*env)->GetByteArrayRegion(env, key, 0, keyLen, keyBytes);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    (*env)->GetByteArrayRegion(env, iv, 0, ivLen, ivBytes);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    // Before 26.4, the protocol used the shared secret as both the key and the IV. That is the
    // caller's business now: the Java side passes the key as the IV for those older protocols.
    result = CCCryptorCreateWithMode(encrypt ? kCCEncrypt : kCCDecrypt,
        kCCModeCFB8,
        kCCAlgorithmAES128,
        ccNoPadding,
        ivBytes,
        keyBytes,
        16,
        NULL,
        0,
        0,
        0,
        &cryptor);
    if (result != kCCSuccess) {
        throwException(env, "java/security/GeneralSecurityException",
            "commoncrypto initialize cipher");
        goto cleanup;
    }
    ret = (jlong) cryptor;

cleanup:
    memset_s(keyBytes, sizeof(keyBytes), 0, sizeof(keyBytes));
    memset_s(ivBytes, sizeof(ivBytes), 0, sizeof(ivBytes));
    return ret;
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