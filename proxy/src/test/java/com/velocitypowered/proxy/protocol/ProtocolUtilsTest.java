/*
 * Copyright (C) 2018 Velocity Contributors
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

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
