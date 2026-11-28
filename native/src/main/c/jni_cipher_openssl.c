#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <openssl/crypto.h>
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
    EVP_CIPHER_CTX *ctx = NULL;

    (*env)->GetByteArrayRegion(env, key, 0, keyLen, keyBytes);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    (*env)->GetByteArrayRegion(env, iv, 0, ivLen, ivBytes);
    if ((*env)->ExceptionCheck(env)) {
        goto cleanup;
    }

    ctx = EVP_CIPHER_CTX_new();
    if (ctx == NULL) {
        throwException(env, "java/lang/OutOfMemoryError", "allocate cipher");
        goto cleanup;
    }

    // Before 26.4, the protocol used the shared secret as both the key and the IV. That is the
    // caller's business now: the Java side passes the key as the IV for those older protocols.
    if (EVP_CipherInit(ctx, EVP_aes_128_cfb8(), (byte*) keyBytes, (byte*) ivBytes,
            encrypt) != 1) {
        EVP_CIPHER_CTX_free(ctx);
        throwException(env, "java/security/GeneralSecurityException", "openssl initialize cipher");
        goto cleanup;
    }
    ret = (jlong) ctx;

cleanup:
    OPENSSL_cleanse(keyBytes, sizeof(keyBytes));
    OPENSSL_cleanse(ivBytes, sizeof(ivBytes));
    return ret;
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