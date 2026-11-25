#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <openssl/evp.h>
#include "jni_util.h"

typedef unsigned char byte;

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_encryption_OpenSslCipherImpl_init(JNIEnv *env,
    jclass clazz,
    jbyteArray key,
    jbyteArray iv,
    jboolean encrypt)
{
    jsize keyLen = (*env)->GetArrayLength(env, key);
    if (keyLen != 16) {
        throwException(env, "java/lang/IllegalArgumentException", "cipher not 16 bytes");
        return 0;
    }

    jsize ivLen = (*env)->GetArrayLength(env, iv);
    if (ivLen != 16) {
        throwException(env, "java/lang/IllegalArgumentException", "IV not 16 bytes");
        return 0;
    }

    // Since we know the array size is always bounded, we can just use Get<Primitive>ArrayRegion
    // and save ourselves some error-checking headaches.
    jbyte keyBytes[16];
    (*env)->GetByteArrayRegion(env, key, 0, keyLen, (jbyte*) keyBytes);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    jbyte ivBytes[16];
    (*env)->GetByteArrayRegion(env, iv, 0, ivLen, (jbyte*) ivBytes);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (ctx == NULL) {
        throwException(env, "java/lang/OutOfMemoryError", "allocate cipher");
        return 0;
    }

    // Before 26.4, the protocol used the shared secret as both the key and the IV. That is the
    // caller's business now: the Java side passes the key as the IV for those older protocols.
    int result = EVP_CipherInit(ctx, EVP_aes_128_cfb8(), (byte*) keyBytes, (byte*) ivBytes,
        encrypt);
    if (result != 1) {
        EVP_CIPHER_CTX_free(ctx);
        throwException(env, "java/security/GeneralSecurityException", "openssl initialize cipher");
        return 0;
    }
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_encryption_OpenSslCipherImpl_free(JNIEnv *env,
    jclass clazz,
    jlong ptr)
{
    EVP_CIPHER_CTX_free((EVP_CIPHER_CTX *) ptr);
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_encryption_OpenSslCipherImpl_process(JNIEnv *env,
    jclass clazz,
    jlong ptr,
    jlong source,
    jint len,
    jlong dest)
{
    EVP_CipherUpdate((EVP_CIPHER_CTX*) ptr, (byte*) dest, &len, (byte*) source, len);
}