/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

/**
 * Protocol utils test.
 */
public class ProtocolUtilsTest {

  @Test
  void negativeVarIntBytes() {
    assertEquals(5, ProtocolUtils.varIntBytes(-1));
    assertEquals(5, ProtocolUtils.varIntBytes(Integer.MIN_VALUE));
  }

  @Test
  void zeroVarIntBytes() {
    assertEquals(1, ProtocolUtils.varIntBytes(0));
    assertEquals(1, ProtocolUtils.varIntBytes(1));
  }

  @Test
  void ensureConsistencyAcrossNumberBits() {
    for (int i = 0; i <= 31; i++) {
      int number = (1 << i) - 1;
      assertEquals(conventionalWrittenBytes(number), ProtocolUtils.varIntBytes(number),
          "mismatch with " + i + "-bit number");
    }
  }

  @Test
  void testPositiveOld() {
    ByteBuf buf = Unpooled.buffer(5);
    for (int i = 0; i >= 0; i += 127) {
      writeReadTestOld(buf, i);
    }
  }

  @Test
  void testNegativeOld() {
    ByteBuf buf = Unpooled.buffer(5);
    for (int i = 0; i <= 0; i -= 127) {
      writeReadTestOld(buf, i);
    }
  }

  private void writeReadTestOld(ByteBuf buf, int test) {
    buf.clear();
    writeVarIntOld(buf, test);
    assertEquals(test, ProtocolUtils.readVarIntSafely(buf));
  }

  @Test
  void test3Bytes() {
    ByteBuf buf = Unpooled.buffer(5);
    for (int i = 0; i < 2097152; i += 31) {
      writeReadTest3Bytes(buf, i);
    }
  }

  private void writeReadTest3Bytes(ByteBuf buf, int test) {
    buf.clear();
    ProtocolUtils.write21BitVarInt(buf, test);
    assertEquals(test, ProtocolUtils.readVarInt(buf));
  }

  @Test
  void testBytesWrittenAtBitBoundaries() {
    ByteBuf varintNew = Unpooled.buffer(5);
    ByteBuf varintOld = Unpooled.buffer(5);

    long bytesNew = 0;
    long bytesOld = 0;
    for (int bit = 0; bit <= 31; bit++) {
      int i = (1 << bit) - 1;

      writeVarIntOld(varintOld, i);
      ProtocolUtils.writeVarInt(varintNew, i);
      assertArrayEquals(ByteBufUtil.getBytes(varintOld), ByteBufUtil.getBytes(varintNew),
          "Encoding of " + i + " was invalid");

      assertEquals(i, oldReadVarIntSafely(varintNew));
      assertEquals(i, ProtocolUtils.readVarIntSafely(varintOld));

      varintNew.clear();
      varintOld.clear();
    }
    assertEquals(bytesNew, bytesOld, "byte sizes differ");
  }

  @Test
  void testBytesWritten() {
    ByteBuf varintNew = Unpooled.buffer(5);
    ByteBuf varintOld = Unpooled.buffer(5);

    long bytesNew = 0;
    long bytesOld = 0;
    for (int i = 0; i <= 1_000_000; i++) {
      ProtocolUtils.writeVarInt(varintNew, i);
      writeVarIntOld(varintOld, i);
      bytesNew += varintNew.readableBytes();
      bytesOld += varintOld.readableBytes();
      varintNew.clear();
      varintOld.clear();
    }
    assertEquals(bytesNew, bytesOld, "byte sizes differ");
  }

  private static int oldReadVarIntSafely(ByteBuf buf) {
    int i = 0;
    int maxRead = Math.min(5, buf.readableBytes());
    for (int j = 0; j < maxRead; j++) {
      int k = buf.readByte();
      i |= (k & 0x7F) << j * 7;
      if ((k & 0x80) != 128) {
        return i;
      }
    }
    return Integer.MIN_VALUE;
  }

  private void writeVarIntOld(ByteBuf buf, int value) {
    while (true) {
      if ((value & 0xFFFFFF80) == 0) {
        buf.writeByte(value);
        return;
      }

      buf.writeByte(value & 0x7F | 0x80);
      value >>>= 7;
    }
  }

  private int conventionalWrittenBytes(int value) {
    int wouldBeWritten = 0;
    while (true) {
      if ((value & ~0x7FL) == 0) {
        wouldBeWritten++;
        return wouldBeWritten;
      } else {
        wouldBeWritten++;
        value >>>= 7;
      }
    }
  }
}
