package com.velocitypowered.natives.util;

import com.velocitypowered.natives.Native;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

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
    if (nativeStuff.preferredBufferType() != BufferPreference.DIRECT_REQUIRED
        || buf.hasMemoryAddress()) {
      return buf.retain();
    }

    // It's not, so we must make a direct copy.
    ByteBuf newBuf = alloc.directBuffer(buf.readableBytes());
    newBuf.writeBytes(buf);
    return newBuf;
  }

  /**
   * Creates a {@link ByteBuf} that will have the best performance with the specified
   * {@code nativeStuff}.
   *
   * @param alloc the {@link ByteBufAllocator} to use
   * @param nativeStuff the native we are working with
   * @return a buffer compatible with the native
   */
  public static ByteBuf preferredBuffer(ByteBufAllocator alloc, Native nativeStuff) {
    return nativeStuff.preferredBufferType() != BufferPreference.HEAP_PREFERRED
        ? alloc.directBuffer() : alloc.heapBuffer();
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
    return nativeStuff.preferredBufferType() != BufferPreference.HEAP_PREFERRED
        ? alloc.directBuffer(initialCapacity) : alloc.heapBuffer(initialCapacity);
  }
}
