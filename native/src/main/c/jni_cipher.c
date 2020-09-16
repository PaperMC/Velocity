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
    jboolean encrypt)
{
    jsize keyLen = (*env)->GetArrayLength(env, key);
    if (keyLen != 16) {
        throwException(env, "java/lang/IllegalArgumentException", "cipher not 16 bytes");
        return 0;
    }

    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (ctx == NULL) {
        throwException(env, "java/lang/OutOfMemoryError", "allocate cipher");
        return 0;
    }

    // Since we know the array size is always bounded, we can just use Get<Primitive>ArrayRegion
    // and save ourselves some error-checking headaches.
    jbyte keyBytes[16];
    (*env)->GetByteArrayRegion(env, key, 0, keyLen, (jbyte*) keyBytes);
    if ((*env)->ExceptionCheck(env)) {
        return 0;
    }

    int result = EVP_CipherInit(ctx, EVP_aes_128_cfb8(), (byte*) keyBytes, (byte*) keyBytes,
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