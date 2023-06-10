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

import com.velocitypowered.natives.Disposable;
import com.velocitypowered.natives.Native;
import io.netty.buffer.ByteBuf;
import java.util.zip.DataFormatException;

/**
 * Provides an interface to inflate and deflate {@link ByteBuf}s using zlib or a compatible
 * implementation.
 */
public interface VelocityCompressor extends Disposable, Native {
  void inflate(ByteBuf source, ByteBuf destination, int uncompressedSize)
      throws DataFormatException;

  void deflate(ByteBuf source, ByteBuf destination) throws DataFormatException;
}
