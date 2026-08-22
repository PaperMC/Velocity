/*
 * Copyright (C) 2018-2022 Velocity Contributors
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

package com.velocitypowered.natives.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.condition.OS.LINUX;

import com.velocitypowered.natives.util.BufferPreference;
import com.velocitypowered.natives.util.Natives;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import java.util.Random;
import java.util.function.Supplier;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;

class VelocityCompressorTest {

  private static final byte[] TEST_DATA = new byte[1 << 14];

  @BeforeAll
  static void checkNatives() throws IOException {
    Natives.compress.getLoadedVariant();
    new Random(1).nextBytes(TEST_DATA);
  }

  @Test
  @EnabledOnOs({LINUX})
  void sanityCheckNative() {
    assertThrows(IllegalArgumentException.class, () -> Natives.compress.get().create(-42));
  }

  @Test
  @EnabledOnOs({LINUX})
  void nativeIntegrityCheck() throws DataFormatException {
    VelocityCompressor compressor = Natives.compress.get().create(Deflater.DEFAULT_COMPRESSION);
    if (compressor.preferredBufferType() != BufferPreference.DIRECT_REQUIRED) {
      compressor.close();
      fail("Loaded regular compressor");
    }
    check(compressor, () -> Unpooled.directBuffer(TEST_DATA.length + 32));
  }

  @Test
  void javaIntegrityCheckDirect() throws DataFormatException {
    VelocityCompressor compressor = JavaVelocityCompressor.FACTORY
        .create(Deflater.DEFAULT_COMPRESSION);
    check(compressor, () -> Unpooled.directBuffer(TEST_DATA.length + 32));
  }

  @Test
  void javaIntegrityCheckHeap() throws DataFormatException {
    VelocityCompressor compressor = JavaVelocityCompressor.FACTORY
        .create(Deflater.DEFAULT_COMPRESSION);
    check(compressor, () -> Unpooled.buffer(TEST_DATA.length + 32));
  }

  private static final int BOMB_ACTUAL_SIZE = 1 << 20;
  private static final int BOMB_LYING_CLAIM = 1024;

  @Test
  void javaRejectsUnderReportedUncompressedSize() throws DataFormatException {
    VelocityCompressor compressor = JavaVelocityCompressor.FACTORY
        .create(Deflater.DEFAULT_COMPRESSION);
    DataFormatException ex = assertRejectsDecompressionBomb(compressor);
    // The Java compressor's size guard names the claimed size in its message, so operators can
    // tell an over-size rejection apart from a genuinely corrupt stream.
    assertTrue(ex.getMessage().contains(String.valueOf(BOMB_LYING_CLAIM)),
        "rejection must originate from the uncompressed-size guard, got: " + ex.getMessage());
  }

  @Test
  @EnabledOnOs({LINUX})
  void nativeRejectsUnderReportedUncompressedSize() throws DataFormatException {
    VelocityCompressor compressor = Natives.compress.get().create(Deflater.DEFAULT_COMPRESSION);
    if (compressor.preferredBufferType() != BufferPreference.DIRECT_REQUIRED) {
      compressor.close();
      fail("Loaded regular compressor");
    }
    // libdeflate rejects with its own native-origin message ("uncompressed size is inaccurate"),
    // so we only assert the behavioural guarantee here, not the message text.
    assertRejectsDecompressionBomb(compressor);
  }

  /**
   * Asserts that a compressor refuses a decompression bomb: a small, valid deflate stream whose
   * claimed uncompressed size is far smaller than what it actually inflates to. Verifies the same
   * stream round-trips when the claimed size is honest (proving the rejection is caused by the
   * under-reported size, not corrupt input) and that no output is written past the claimed size.
   * Closes the compressor before returning the exception thrown by the rejected inflate.
   */
  private DataFormatException assertRejectsDecompressionBomb(VelocityCompressor compressor)
      throws DataFormatException {
    // Direct buffers so this works for the native compressor, which requires them.
    ByteBuf source = Unpooled.directBuffer(BOMB_ACTUAL_SIZE);
    ByteBuf compressed = Unpooled.directBuffer();
    try {
      source.writeZero(BOMB_ACTUAL_SIZE);
      compressor.deflate(source, compressed);
      final int compressedSize = compressed.readableBytes();
      assertTrue(compressedSize < BOMB_ACTUAL_SIZE / 100,
          "sanity: payload really is a decompression bomb (" + compressedSize + " -> "
              + BOMB_ACTUAL_SIZE + ")");

      // Positive control: the compressed stream is perfectly valid and round-trips when the peer
      // tells the truth about its uncompressed size.
      ByteBuf honest = Unpooled.directBuffer();
      try {
        compressor.inflate(compressed.duplicate(), honest, BOMB_ACTUAL_SIZE);
        assertEquals(BOMB_ACTUAL_SIZE, honest.readableBytes(),
            "valid stream must fully decompress when the claimed size is honest");
      } finally {
        honest.release();
      }

      // Attack: same valid stream, but a tiny claimed size. inflate must refuse rather than grow
      // the destination without bound.
      ByteBuf decompressed = Unpooled.directBuffer();
      try {
        DataFormatException ex = assertThrows(DataFormatException.class,
            () -> compressor.inflate(compressed.duplicate(), decompressed, BOMB_LYING_CLAIM));
        assertTrue(decompressed.writerIndex() <= BOMB_LYING_CLAIM,
            "inflate must not write past the claimed uncompressed size");
        return ex;
      } finally {
        decompressed.release();
      }
    } finally {
      source.release();
      compressed.release();
      compressor.close();
    }
  }

  private void check(VelocityCompressor compressor, Supplier<ByteBuf> bufSupplier)
      throws DataFormatException {
    ByteBuf source = bufSupplier.get();
    ByteBuf dest = bufSupplier.get();
    ByteBuf decompressed = bufSupplier.get();

    source.writeBytes(TEST_DATA);
    int uncompressedData = source.readableBytes();

    try {
      compressor.deflate(source, dest);
      compressor.inflate(dest, decompressed, uncompressedData);
      source.readerIndex(0);
      assertTrue(ByteBufUtil.equals(source, decompressed));
    } finally {
      source.release();
      dest.release();
      decompressed.release();
      compressor.close();
    }
  }
}