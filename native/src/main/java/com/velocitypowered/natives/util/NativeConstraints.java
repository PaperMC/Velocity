package com.velocitypowered.natives.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.function.BooleanSupplier;

public class NativeConstraints {
  private static final boolean NATIVES_ENABLED = !Boolean.getBoolean("velocity.natives-disabled");
  private static final boolean IS_AMD64;
  private static final boolean IS_AARCH64;
  private static final boolean CAN_GET_MEMORYADDRESS;

  static {
    ByteBuf test = Unpooled.directBuffer();
    try {
      CAN_GET_MEMORYADDRESS = test.hasMemoryAddress();
    } finally {
      test.release();
    }

    String osArch = System.getProperty("os.arch", "");
    // HotSpot on Intel macOS prefers x86_64, but OpenJ9 on macOS and HotSpot/OpenJ9 elsewhere
    // give amd64.
    IS_AMD64 = osArch.equals("amd64") || osArch.equals("x86_64");
    IS_AARCH64 = osArch.equals("aarch64");
  }

  static final BooleanSupplier NATIVE_BASE = () -> NATIVES_ENABLED && CAN_GET_MEMORYADDRESS;

  static final BooleanSupplier LINUX_X86_64 = () -> {
    return NATIVE_BASE.getAsBoolean()
        && System.getProperty("os.name", "").equalsIgnoreCase("Linux")
        && IS_AMD64;
  };

  static final BooleanSupplier LINUX_AARCH64 = () -> {
    return NATIVE_BASE.getAsBoolean()
        && System.getProperty("os.name", "").equalsIgnoreCase("Linux")
        && IS_AARCH64;
  };

  static final BooleanSupplier JAVA_11 = () -> {
    return Double.parseDouble(System.getProperty("java.specification.version")) >= 11;
  };
}
