/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.natives.encryption;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.LINUX;

import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.security.GeneralSecurityException;
import java.util.Random;
import java.util.function.Supplier;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

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
  @EnabledOnOs({LINUX})
  void nativeIntegrityCheck() throws GeneralSecurityException {
    VelocityCipherFactory factory = Natives.cipher.get();
    if (factory == JavaVelocityCipher.FACTORY) {
      fail("Loaded regular cipher");
    }
    check(factory, Unpooled::directBuffer);
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
      decrypt.close();
      encrypt.close();
    }
  }
}
