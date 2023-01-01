/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

import java.util.zip.DataFormatException;

/**
 * Represents a native interface for zlib's inflate functions.
 */
class NativeZlibInflate {

  static native long init();

  static native long free(long ctx);

  static  native boolean process(long ctx, long sourceAddress, int sourceLength,
      long destinationAddress, int destinationLength) throws DataFormatException;
}
