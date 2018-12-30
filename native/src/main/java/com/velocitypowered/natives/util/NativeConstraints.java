package com.velocitypowered.natives.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.BooleanSupplier;

public class NativeConstraints {
  private static final boolean NATIVES_ENABLED = !Boolean.getBoolean("velocity.natives-disabled");
  private static final boolean CAN_GET_MEMORYADDRESS;

  static {
    ByteBuf test = Unpooled.directBuffer();
    try {
      CAN_GET_MEMORYADDRESS = test.hasMemoryAddress();
    } finally {
      test.release();
    }
  }

  static final BooleanSupplier MACOS = () -> {
    return NATIVES_ENABLED
        && CAN_GET_MEMORYADDRESS
        && System.getProperty("os.name", "").equalsIgnoreCase("Mac OS X")
        && System.getProperty("os.arch").equals("x86_64");
  };

  static final BooleanSupplier LINUX = () -> {
    return NATIVES_ENABLED
        && CAN_GET_MEMORYADDRESS
        && System.getProperty("os.name", "").equalsIgnoreCase("Linux")
        && System.getProperty("os.arch").equals("amd64");
  };
}
