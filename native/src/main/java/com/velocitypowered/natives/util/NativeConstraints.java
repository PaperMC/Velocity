package com.velocitypowered.natives.util;

import java.util.function.BooleanSupplier;

public class NativeConstraints {
  private static final boolean NATIVES_ENABLED = !Boolean.getBoolean("velocity.natives-disabled");

  static final BooleanSupplier MACOS = () -> {
    return NATIVES_ENABLED
        && System.getProperty("os.name", "").equalsIgnoreCase("Mac OS X")
        && System.getProperty("os.arch").equals("x86_64");
  };

  static final BooleanSupplier LINUX = () -> {
    return NATIVES_ENABLED
        && System.getProperty("os.name", "").equalsIgnoreCase("Linux")
        && System.getProperty("os.arch").equals("amd64");
  };
}
