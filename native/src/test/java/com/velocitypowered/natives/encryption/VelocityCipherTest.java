package com.velocitypowered.natives.encryption;

import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class VelocityCipherTest {
    private static final int ENCRYPT_DATA_SIZE = 1 << 16;

    @BeforeAll
    static void checkNatives() {
        Natives.cipher.getLoadedVariant();
    }

    @Test
    @Disabled
    void nativeIntegrityCheck() throws GeneralSecurityException {
        VelocityCipherFactory factory = Natives.cipher.get();
        if (factory == JavaVelocityCipher.FACTORY) {
            fail("Loaded regular compressor");
        }
        check(factory);
    }

    @Test
    void javaIntegrityCheck() throws GeneralSecurityException {
        check(JavaVelocityCipher.FACTORY);
    }

    private void check(VelocityCipherFactory factory) throws GeneralSecurityException {
        // Generate a random 16-byte key.
        Random random = new Random(1);
        byte[] key = new byte[16];
        random.nextBytes(key);

        VelocityCipher decrypt = factory.forDecryption(new SecretKeySpec(key, "AES"));
        VelocityCipher encrypt = factory.forEncryption(new SecretKeySpec(key, "AES"));

        ByteBuf source = Unpooled.directBuffer(ENCRYPT_DATA_SIZE);
        ByteBuf dest = Unpooled.directBuffer(ENCRYPT_DATA_SIZE);
        ByteBuf decryptionBuf = Unpooled.directBuffer(ENCRYPT_DATA_SIZE);

        byte[] randomBytes = new byte[ENCRYPT_DATA_SIZE];
        random.nextBytes(randomBytes);
        source.writeBytes(randomBytes);

        try {
            encrypt.process(source, dest);
            decrypt.process(dest, decryptionBuf);
            source.readerIndex(0);
            assertTrue(ByteBufUtil.equals(source, decryptionBuf));
        } finally {
            source.release();
            dest.release();
            decryptionBuf.release();
            decrypt.dispose();
            encrypt.dispose();
        }
    }
}
