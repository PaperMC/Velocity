package com.velocitypowered.natives.encryption;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.function.Supplier;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class VelocityCipherTest {

  private static final int ENCRYPT_DATA_SIZE = 1 << 14;
  private static byte[] TEST_DATA = new byte[ENCRYPT_DATA_SIZE];
  private static final byte[] AES_KEY = new byte[16];

  @BeforeAll
  static void checkNatives() {
    Natives.cipher.getLoadedVariant();
    Random random = new Random(1);
    random.nextBytes(TEST_DATA);
    random.nextBytes(AES_KEY);
  }

  @Test
  @Disabled
  void nativeIntegrityCheck() throws GeneralSecurityException {
    VelocityCipherFactory factory = Natives.cipher.get();
    if (factory == JavaVelocityCipher.FACTORY) {
      fail("Loaded regular cipher");
    }
    check(factory, Unpooled::directBuffer);
  }

  @Test
  void javaIntegrityCheckDirect() throws GeneralSecurityException {
    check(JavaVelocityCipher.FACTORY, Unpooled::directBuffer);
  }

  @Test
  void javaIntegrityCheckHeap() throws GeneralSecurityException {
    check(JavaVelocityCipher.FACTORY, Unpooled::buffer);
  }

  private void check(VelocityCipherFactory factory, Supplier<ByteBuf> bufSupplier)
      throws GeneralSecurityException {
    // Generate a random 16-byte key.
    VelocityCipher decrypt = factory.forDecryption(new SecretKeySpec(AES_KEY, "AES"));
    VelocityCipher encrypt = factory.forEncryption(new SecretKeySpec(AES_KEY, "AES"));

    ByteBuf source = bufSupplier.get();

    source.writeBytes(TEST_DATA);

    ByteBuf workingBuf = source.copy();

    try {
      encrypt.process(workingBuf);
      decrypt.process(workingBuf);
      assertTrue(ByteBufUtil.equals(source, workingBuf));
    } finally {
      source.release();
      workingBuf.release();
      decrypt.dispose();
      encrypt.dispose();
    }
  }
}
