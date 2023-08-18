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