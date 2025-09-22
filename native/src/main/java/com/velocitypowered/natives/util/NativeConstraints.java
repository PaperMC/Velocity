/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.natives.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/**
 * Statically-computed constraints for native code.
 */
public class NativeConstraints {
  private static final boolean NATIVES_ENABLED = !Boolean.getBoolean("velocity.natives-disabled");
  private static final boolean IS_AMD64;
  private static final boolean IS_AARCH64;
  private static final boolean CAN_GET_MEMORYADDRESS;
  private static final boolean IS_LINUX;
  private static final boolean IS_MUSL_LIBC;

  static {
    ByteBuf test = Unpooled.directBuffer();
    try {
      CAN_GET_MEMORYADDRESS = test.hasMemoryAddress();
    } finally {
      test.release();
    }

    String osArch = System.getProperty("os.arch", "");
    IS_AMD64 = osArch.equals("amd64") || osArch.equals("x86_64");
    IS_AARCH64 = osArch.equals("aarch64") || osArch.equals("arm64");

    IS_LINUX = System.getProperty("os.name", "").equalsIgnoreCase("Linux");

    // Determine if we're using musl libc by invoking `ldd --version`.
    if (IS_LINUX) {
      boolean isMusl;
      try {
        Process process = new ProcessBuilder("ldd", "--version")
            .redirectErrorStream(true)
            .start();
        process.waitFor();
        try (var reader = process.getInputStream()) {
          byte[] outputRaw = reader.readAllBytes();
          String output = new String(outputRaw, StandardCharsets.UTF_8);
          isMusl = output.contains("musl");
        }
      } catch (Exception e) {
        isMusl = false;
      }
      IS_MUSL_LIBC = isMusl;
    } else {
      IS_MUSL_LIBC = false;
    }
  }

  static final BooleanSupplier NATIVE_BASE = () -> NATIVES_ENABLED && CAN_GET_MEMORYADDRESS;

  static final BooleanSupplier LINUX_X86_64 = () -> NATIVE_BASE.getAsBoolean()
      && IS_LINUX && IS_AMD64 && !IS_MUSL_LIBC;

  static final BooleanSupplier LINUX_X86_64_MUSL = () -> NATIVE_BASE.getAsBoolean()
      && IS_LINUX && IS_AMD64 && IS_MUSL_LIBC;

  static final BooleanSupplier LINUX_AARCH64 = () -> NATIVE_BASE.getAsBoolean()
      && IS_LINUX && IS_AARCH64 && !IS_MUSL_LIBC;

  static final BooleanSupplier LINUX_AARCH64_MUSL = () -> NATIVE_BASE.getAsBoolean()
      && IS_LINUX && IS_AARCH64 && IS_MUSL_LIBC;

  static final BooleanSupplier MACOS_AARCH64 = () -> NATIVE_BASE.getAsBoolean()
      && System.getProperty("os.name", "").equalsIgnoreCase("Mac OS X")
      && IS_AARCH64;
}
