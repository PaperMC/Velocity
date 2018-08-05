#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <mbedtls/aes.h>
#include "jni_util.h"

typedef unsigned char byte;

typedef struct {
    mbedtls_aes_context cipher;
    byte *key;
} velocity_cipher_context;

JNIEXPORT jlong JNICALL
Java_com_velocitypowered_natives_encryption_MbedtlsAesImpl_init(JNIEnv *env,
    jobject obj,
    jbyteArray key)
{
    velocity_cipher_context *ctx = malloc(sizeof(velocity_cipher_context));
    if (ctx == NULL) {
        throwException(env, "java/lang/OutOfMemoryError", "cipher allocate context");
        return 0;
    }

    jsize keyLen = (*env)->GetArrayLength(env, key);
    jbyte* keyBytes = (*env)->GetPrimitiveArrayCritical(env, key, NULL);
    if (keyBytes == NULL) {
        free(ctx);
        throwException(env, "java/lang/OutOfMemoryError", "cipher get key");
        return 0;
    }

    mbedtls_aes_init(&ctx->cipher);
    int ret = mbedtls_aes_setkey_enc(&ctx->cipher, (byte*) keyBytes, keyLen * 8);
    if (ret != 0) {
        (*env)->ReleasePrimitiveArrayCritical(env, key, keyBytes, 0);
        mbedtls_aes_free(&ctx->cipher);
        free(ctx);

        throwException(env, "java/security/GeneralSecurityException", "mbedtls set aes key");
        return 0;
    }

    ctx->key = malloc(keyLen);
    if (ctx->key == NULL) {
        (*env)->ReleasePrimitiveArrayCritical(env, key, keyBytes, 0);
        mbedtls_aes_free(&ctx->cipher);
        free(ctx);

        throwException(env, "java/lang/OutOfMemoryError", "cipher copy key");
        return 0;
    }
    memcpy(ctx->key, keyBytes, keyLen);
    (*env)->ReleasePrimitiveArrayCritical(env, key, keyBytes, 0);
    return (jlong) ctx;
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_encryption_MbedtlsAesImpl_free(JNIEnv *env,
    jobject obj,
    jlong ptr)
{
    velocity_cipher_context *ctx = (velocity_cipher_context*) ptr;
    mbedtls_aes_free(&ctx->cipher);
    free(ctx->key);
    free(ctx);
}

JNIEXPORT void JNICALL
Java_com_velocitypowered_natives_encryption_MbedtlsAesImpl_process(JNIEnv *env,
    jobject obj,
    jlong ptr,
    jlong source,
    jint len,
    jlong dest,
    jboolean encrypt)
{
    velocity_cipher_context *ctx = (velocity_cipher_context*) ptr;
    mbedtls_aes_crypt_cfb8(&ctx->cipher, encrypt ? MBEDTLS_AES_ENCRYPT : MBEDTLS_AES_DECRYPT, len, ctx->key,
        (byte*) source, (byte*) dest);
}