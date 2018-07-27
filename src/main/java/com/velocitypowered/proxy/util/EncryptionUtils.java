package com.velocitypowered.proxy.util;

import javax.crypto.Cipher;
import java.math.BigInteger;
import java.security.*;

public enum EncryptionUtils { ;
    public static String twosComplementSha1Digest(byte[] digest) {
        return new BigInteger(digest).toString(16);
    }

    public static byte[] decryptRsa(KeyPair keyPair, byte[] bytes) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA");
        cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
        return cipher.doFinal(bytes);
    }

    public static String generateServerId(byte[] sharedSecret, PublicKey key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            digest.update(sharedSecret);
            digest.update(key.getEncoded());
            return twosComplementSha1Digest(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
