/*
 * Copyright (C) 2019-2023 Velocity Contributors
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

/**
 * Shared helpers for zlib-compatible compression buffers.
 */
public final class CompressorUtils {

  /**
   * The default preferred output buffer size for zlib.
   */
  static final int ZLIB_BUFFER_SIZE = 8192;

  /**
   * Upper bound on the size of a zlib stream produced from {@code inputLength} uncompressed bytes,
   * i.e. libdeflate/zlib {@code compressBound}. Used to pre-size output buffers so a compressor's
   * grow-loop (which discards a full compression pass on insufficient room and would double the
   * measured latency) never triggers. The formula matches libdeflate's {@code libdeflate_zlib_compress_bound}:
   * roughly {@code input + input/2 + 128} of structural overhead for the dominant sub-64KiB range.
   *
   * @param inputLength the uncompressed input size
   * @return an upper bound on the compressed output size
   */
  public static int compressBound(final int inputLength) {
    return inputLength + (inputLength >>> 1) + 128;
  }

  private CompressorUtils() {
    throw new AssertionError();
  }
}
