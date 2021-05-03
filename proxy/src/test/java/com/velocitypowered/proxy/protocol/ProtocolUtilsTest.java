package com.velocitypowered.proxy.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ProtocolUtilsTest {

  @Test
  void negativeVarIntBytes() {
    assertEquals(5, ProtocolUtils.varintBytes(-1));
    assertEquals(5, ProtocolUtils.varintBytes(Integer.MIN_VALUE));
  }

  @Test
  void zeroVarIntBytes() {
    assertEquals(1, ProtocolUtils.varintBytes(0));
    assertEquals(1, ProtocolUtils.varintBytes(1));
  }

  @Test
  void ensureConsistencyAcrossNumberBits() {
    for (int i = 0; i <= 31; i++) {
      int number = (1 << i) - 1;
      assertEquals(conventionalWrittenBytes(number), ProtocolUtils.varintBytes(number),
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
