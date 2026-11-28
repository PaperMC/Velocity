/*
 * Copyright (C) 2018-2026 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.junit.jupiter.api.condition.OS.LINUX;

import com.velocitypowered.natives.util.BufferPreference;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Random;
import java.util.stream.Stream;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests the Java cipher and, when one is loaded on this host, the native cipher.
 */
class VelocityCipherTest {

  private static final int ENCRYPT_DATA_SIZE = 1 << 14;
  private static final byte[] TEST_DATA = new byte[ENCRYPT_DATA_SIZE];
  private static final byte[] AES_KEY = new byte[16];
  private static final byte[] AES_IV = new byte[16];

  // NIST SP 800-38A, F.3.7 (CFB8-AES128.Encrypt) and F.3.8 (CFB8-AES128.Decrypt)
  private static final byte[] NIST_KEY = hex("2b7e151628aed2a6abf7158809cf4f3c");
  private static final byte[] NIST_IV = hex("000102030405060708090a0b0c0d0e0f");
  private static final byte[] NIST_PLAINTEXT = hex("6bc1bee22e409f96e93d7e117393172aae2d");
  private static final byte[] NIST_CIPHERTEXT = hex("3b79424c9c0dd436bace9e0ed4586a4f32b9");

  @BeforeAll
  static void setUp() {
    Natives.cipher.getLoadedVariant();
    Random random = new Random(1);
    random.nextBytes(TEST_DATA);
    random.nextBytes(AES_KEY);
    random.nextBytes(AES_IV);
  }

  /**
   * The Java cipher, plus the native cipher if this host loaded one.
   */
  static Stream<Arguments> factories() {
    Stream.Builder<Arguments> builder = Stream.builder();
    builder.add(Arguments.of("Java", JavaVelocityCipher.FACTORY));
    if (Natives.cipher.get() != JavaVelocityCipher.FACTORY) {
      builder.add(Arguments.of(Natives.cipher.getLoadedVariant(), Natives.cipher.get()));
    }
    return builder.build();
  }

  @Test
  @EnabledOnOs({LINUX})
  void nativeLoadedOnLinux() {
    assertNotSame(JavaVelocityCipher.FACTORY, Natives.cipher.get(),
        "Loaded the Java cipher instead of a native one");
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void roundTripKeyAsIv(String name, VelocityCipherFactory factory)
      throws GeneralSecurityException {
    SecretKey key = aesKey(AES_KEY);
    assertRoundTrip(factory.forEncryption(key), factory.forDecryption(key));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void roundTripWithIv(String name, VelocityCipherFactory factory)
      throws GeneralSecurityException {
    SecretKey key = aesKey(AES_KEY);
    assertRoundTrip(factory.forEncryption(key, AES_IV), factory.forDecryption(key, AES_IV));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void nistEncrypt(String name, VelocityCipherFactory factory) throws GeneralSecurityException {
    assertArrayEquals(NIST_CIPHERTEXT,
        process(factory.forEncryption(aesKey(NIST_KEY), NIST_IV), NIST_PLAINTEXT));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void nistDecrypt(String name, VelocityCipherFactory factory) throws GeneralSecurityException {
    assertArrayEquals(NIST_PLAINTEXT,
        process(factory.forDecryption(aesKey(NIST_KEY), NIST_IV), NIST_CIPHERTEXT));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void noIvUsesKeyAsIv(String name, VelocityCipherFactory factory)
      throws GeneralSecurityException {
    SecretKey key = aesKey(AES_KEY);
    assertArrayEquals(
        process(factory.forEncryption(key, AES_KEY), TEST_DATA),
        process(factory.forEncryption(key), TEST_DATA));
    assertArrayEquals(
        process(factory.forEncryption(key, AES_KEY), TEST_DATA),
        process(factory.forEncryption(key, null), TEST_DATA));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void ivChangesOutput(String name, VelocityCipherFactory factory)
      throws GeneralSecurityException {
    SecretKey key = aesKey(AES_KEY);
    byte[] withKeyAsIv = process(factory.forEncryption(key), TEST_DATA);
    byte[] withIv = process(factory.forEncryption(key, AES_IV), TEST_DATA);
    assertFalse(Arrays.equals(withKeyAsIv, withIv));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void keepsStateBetweenCalls(String name, VelocityCipherFactory factory)
      throws GeneralSecurityException {
    SecretKey key = aesKey(AES_KEY);
    byte[] oneShot = process(factory.forEncryption(key, AES_IV), TEST_DATA);

    VelocityCipher cipher = factory.forEncryption(key, AES_IV);
    byte[] chunked = new byte[TEST_DATA.length];
    try {
      // uneven chunk sizes, so chunks don't line up with the AES block size
      int offset = 0;
      int chunk = 1;
      while (offset < TEST_DATA.length) {
        int len = Math.min(chunk, TEST_DATA.length - offset);
        ByteBuf buf = allocate(cipher, len);
        try {
          buf.writeBytes(TEST_DATA, offset, len);
          cipher.process(buf);
          buf.readBytes(chunked, offset, len);
        } finally {
          buf.release();
        }
        offset += len;
        chunk = chunk * 3 + 1;
      }
    } finally {
      cipher.close();
    }
    assertArrayEquals(oneShot, chunked);
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("factories")
  void rejectsWrongIvLength(String name, VelocityCipherFactory factory) {
    SecretKey key = aesKey(AES_KEY);
    assertThrows(IllegalArgumentException.class, () -> factory.forEncryption(key, new byte[15]));
    assertThrows(IllegalArgumentException.class, () -> factory.forDecryption(key, new byte[17]));
  }

  @Test
  void nativeRejectsBadArrays() {
    assumeTrue(Natives.cipher.get() != JavaVelocityCipher.FACTORY, "No native cipher on this host");

    // These would crash the JVM rather than throw if the native code didn't check them itself.
    assertThrows(NullPointerException.class, () -> OpenSslCipherImpl.init(null, AES_IV, true));
    assertThrows(NullPointerException.class, () -> OpenSslCipherImpl.init(AES_KEY, null, true));
    assertThrows(IllegalArgumentException.class,
        () -> OpenSslCipherImpl.init(new byte[15], AES_IV, true));
    assertThrows(IllegalArgumentException.class,
        () -> OpenSslCipherImpl.init(AES_KEY, new byte[17], true));
  }

  @Test
  void nativeMatchesJava() throws GeneralSecurityException {
    VelocityCipherFactory nativeFactory = Natives.cipher.get();
    assumeTrue(nativeFactory != JavaVelocityCipher.FACTORY, "No native cipher on this host");

    SecretKey key = aesKey(AES_KEY);
    assertArrayEquals(
        process(JavaVelocityCipher.FACTORY.forEncryption(key), TEST_DATA),
        process(nativeFactory.forEncryption(key), TEST_DATA));
    assertArrayEquals(
        process(JavaVelocityCipher.FACTORY.forEncryption(key, AES_IV), TEST_DATA),
        process(nativeFactory.forEncryption(key, AES_IV), TEST_DATA));

    // and across implementations: Java encrypts, the native cipher decrypts and vice versa
    assertArrayEquals(TEST_DATA, process(nativeFactory.forDecryption(key, AES_IV),
        process(JavaVelocityCipher.FACTORY.forEncryption(key, AES_IV), TEST_DATA)));
    assertArrayEquals(TEST_DATA, process(JavaVelocityCipher.FACTORY.forDecryption(key, AES_IV),
        process(nativeFactory.forEncryption(key, AES_IV), TEST_DATA)));
  }

  private static void assertRoundTrip(VelocityCipher encrypt, VelocityCipher decrypt) {
    try {
      byte[] encrypted = process(encrypt, TEST_DATA, false);
      assertFalse(Arrays.equals(TEST_DATA, encrypted));
      assertArrayEquals(TEST_DATA, process(decrypt, encrypted, false));
    } finally {
      encrypt.close();
      decrypt.close();
    }
  }

  // Runs the data through the cipher and closes it.
  private static byte[] process(VelocityCipher cipher, byte[] data) {
    return process(cipher, data, true);
  }

  private static byte[] process(VelocityCipher cipher, byte[] data, boolean close) {
    ByteBuf buf = allocate(cipher, data.length);
    try {
      buf.writeBytes(data);
      cipher.process(buf);
      return ByteBufUtil.getBytes(buf);
    } finally {
      buf.release();
      if (close) {
        cipher.close();
      }
    }
  }

  private static ByteBuf allocate(VelocityCipher cipher, int capacity) {
    BufferPreference preference = cipher.preferredBufferType();
    return preference == BufferPreference.DIRECT_REQUIRED
        || preference == BufferPreference.DIRECT_PREFERRED
        ? Unpooled.directBuffer(capacity) : Unpooled.buffer(capacity);
  }

  private static SecretKey aesKey(byte[] key) {
    return new SecretKeySpec(key, "AES");
  }

  private static byte[] hex(String hex) {
    return HexFormat.of().parseHex(hex);
  }
}
