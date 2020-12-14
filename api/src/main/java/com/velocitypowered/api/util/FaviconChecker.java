package com.velocitypowered.api.util;

import java.nio.ByteBuffer;

class FaviconChecker {
  private static final byte[] PNG_MAGIC = new byte[] {
      (byte) 137, 80, 78, 71, 13, 10, 26, 10
  };
  private static final byte[] IHDR_NAME = new byte[] {
      73, 72, 68, 82
  };

  public static boolean check(byte[] data) {
    ByteBuffer buf = ByteBuffer.wrap(data);

    for (byte value : PNG_MAGIC) {
      if (buf.get() != value) {
        return false;
      }
    }

    buf.position(buf.position() + 4);
    for (byte value : IHDR_NAME) {
      if (buf.get() != value) {
        return false;
      }
    }

    int width = buf.getInt();
    int height = buf.getInt();

    return width == 64 && height == 64;
  }
}
