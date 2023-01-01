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

import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

class CompressorUtils {
  /**
   * The default preferred output buffer size for zlib.
   */
  static final int ZLIB_BUFFER_SIZE = 8192;

  /**
   * Ensures that the buffer does not go over {@code max}.
   *
   * @param buf the buffer for check
   * @param max the maximum size for the buffer
   * @throws DataFormatException if the buffer becomes too bug
   */
  static void ensureMaxSize(ByteBuf buf, int max) throws DataFormatException {
    int len = buf.readableBytes();
    if (len > max) {
      throw new DataFormatException("Got too much data (" + len + " > " + max + ")");
    }
  }

  private CompressorUtils() {
    throw new AssertionError();
  }
}
