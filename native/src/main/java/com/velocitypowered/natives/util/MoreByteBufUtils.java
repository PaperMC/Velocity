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

import com.velocitypowered.natives.Native;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

/**
 * Additional utilities for {@link ByteBuf}.
 */
public class MoreByteBufUtils {
  private MoreByteBufUtils() {
    throw new AssertionError();
  }

  /**
   * Ensures the {@code buf} will work with the specified {@code nativeStuff}. After this function
   * is called, you should decrement the reference count on the {@code buf} with
   * {@link ByteBuf#release()}.
   *
   * @param alloc the {@link ByteBufAllocator} to use
   * @param nativeStuff the native we are working with
   * @param buf the buffer we are working with
   * @return a buffer compatible with the native
   */
  public static ByteBuf ensureCompatible(ByteBufAllocator alloc, Native nativeStuff, ByteBuf buf) {
    if (isCompatible(nativeStuff, buf)) {
      return buf.retain();
    }

    // It's not, so we must make a direct copy.
    ByteBuf newBuf = preferredBuffer(alloc, nativeStuff, buf.readableBytes());
    newBuf.writeBytes(buf);
    return newBuf;
  }

  private static boolean isCompatible(Native nativeStuff, ByteBuf buf) {
    BufferPreference preferred = nativeStuff.preferredBufferType();
    switch (preferred) {
      case DIRECT_PREFERRED:
      case HEAP_PREFERRED:
        // The native prefers this type, but doesn't strictly require we provide it.
        return true;
      case DIRECT_REQUIRED:
        return buf.hasMemoryAddress();
      case HEAP_REQUIRED:
        return buf.hasArray();
      default:
        throw new AssertionError("Preferred buffer type unknown");
    }
  }

  /**
   * Creates a {@link ByteBuf} that will have the best performance with the specified
   * {@code nativeStuff}.
   *
   * @param alloc the {@link ByteBufAllocator} to use
   * @param nativeStuff the native we are working with
   * @param initialCapacity the initial capacity to allocate
   * @return a buffer compatible with the native
   */
  public static ByteBuf preferredBuffer(ByteBufAllocator alloc, Native nativeStuff,
      int initialCapacity) {
    switch (nativeStuff.preferredBufferType()) {
      case HEAP_REQUIRED:
      case HEAP_PREFERRED:
        return alloc.heapBuffer(initialCapacity);
      case DIRECT_PREFERRED:
      case DIRECT_REQUIRED:
        return alloc.directBuffer(initialCapacity);
      default:
        throw new AssertionError("Preferred buffer type unknown");
    }
  }
}
