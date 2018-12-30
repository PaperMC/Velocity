package com.velocitypowered.natives.util;

import com.velocitypowered.natives.Native;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

public class MoreByteBufUtils {
  private MoreByteBufUtils() {
    throw new AssertionError();
  }

  /**
   * Ensures the {@code buf} will work with the specified {@code nativeThing}. After this function
   * is called, you should decrement the reference count on the {@code buf} with
   * {@link ByteBuf#release()}.
   *
   * @param alloc the {@link ByteBufAllocator} to use
   * @param nativeStuff the native we are working with
   * @param buf the buffer we are working with
   * @return a buffer compatible with the native
   */
  public static ByteBuf ensureCompatible(ByteBufAllocator alloc, Native nativeStuff, ByteBuf buf) {
    if (!nativeStuff.isNative() || buf.hasMemoryAddress()) {
      // Will always work in either case.
      return buf.retain();
    }

    // It's not, so we must make a memory copy.
    ByteBuf newBuf = alloc.directBuffer();
    newBuf.writeBytes(buf);
    return newBuf;
  }
}
