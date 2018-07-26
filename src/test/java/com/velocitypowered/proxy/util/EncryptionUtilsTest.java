package com.velocitypowered.proxy.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncryptionUtilsTest {
    @Test
    void twosComplementSha1Digest() throws Exception {
        String notchHash = hexDigest("Notch");
        assertEquals("4ed1f46bbe04bc756bcb17c0c7ce3e4632f06a48", notchHash);

        String jebHash = hexDigest("jeb_");
        assertEquals("-7c9d5b0044c130109a5d7b5fb5c317c02b4e28c1", jebHash);
    }

    private String hexDigest(String str) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(str.getBytes(StandardCharsets.UTF_8));
        byte[] digested = digest.digest();
        return EncryptionUtils.twosComplementSha1Digest(digested);
    }
}
