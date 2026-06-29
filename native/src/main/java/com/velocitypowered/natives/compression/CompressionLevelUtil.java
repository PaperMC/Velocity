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

package com.velocitypowered.natives.compression;

import java.util.zip.Deflater;

/**
 * Normalizes user-provided compression levels for the available compressor implementations.
 */
public final class CompressionLevelUtil {

  private static final int MIN_LEVEL = 1;
  /** Maximum level supported by the Java {@link Deflater}-based compressor. */
  public static final int JAVA_MAX_LEVEL = 9;
  /** Maximum level supported by the native libdeflate compressor. */
  public static final int LIBDEFLATE_MAX_LEVEL = 12;
  // libdeflate's compression ratio saturates well below its maximum level on real Minecraft
  // traffic (chunks, NBT, chat) while its CPU cost keeps climbing linearly. The shipped benchmark
  // shows level 6 already reaching the plateau at a fraction of the level 12 latency, so the
  // proxy-preferred aggressive default targets the ratio sweet spot rather than the absolute floor.
  /** The level resolved from {@link Deflater#DEFAULT_COMPRESSION} on the native backend. */
  public static final int AGGRESSIVE_NATIVE_DEFAULT = 6;
  /** The level resolved from {@link Deflater#DEFAULT_COMPRESSION} on the Java backend. */
  public static final int AGGRESSIVE_JAVA_DEFAULT = 6;

  private CompressionLevelUtil() {
    throw new AssertionError();
  }

  static int forLibdeflate(int requestedLevel) {
    if (requestedLevel == Deflater.DEFAULT_COMPRESSION) {
      return AGGRESSIVE_NATIVE_DEFAULT;
    }
    return Math.max(MIN_LEVEL, Math.min(LIBDEFLATE_MAX_LEVEL, requestedLevel));
  }

  static int forJava(int requestedLevel) {
    if (requestedLevel == Deflater.DEFAULT_COMPRESSION) {
      return AGGRESSIVE_JAVA_DEFAULT;
    }
    return Math.max(MIN_LEVEL, Math.min(JAVA_MAX_LEVEL, requestedLevel));
  }
}
