package com.velocitypowered.proxy.protocol.util;

import com.google.common.base.Strings;
import io.netty.handler.codec.CorruptedFrameException;

/**
 * Extends {@link com.google.common.base.Preconditions} for Netty's {@link CorruptedFrameException}.
 */
public class NettyPreconditions {
  private NettyPreconditions() {
    throw new AssertionError();
  }

  /**
   * Throws {@link CorruptedFrameException} if {@code b} is false.
   * @param b the expression to check
   * @param message the message to include in the thrown {@link CorruptedFrameException}
   */
  public static void checkFrame(boolean b, String message) {
    if (!b) {
      throw new CorruptedFrameException(message);
    }
  }

  /**
   * Throws {@link CorruptedFrameException} if {@code b} is false.
   * @param b the expression to check
   * @param message the message to include in the thrown {@link CorruptedFrameException}, formatted
   *                like {@link com.google.common.base.Preconditions#checkArgument(boolean)} and
   *                friends
   * @param arg1 the first argument to format the message with
   */
  public static void checkFrame(boolean b, String message, Object arg1) {
    if (!b) {
      throw new CorruptedFrameException(Strings.lenientFormat(message, arg1));
    }
  }

  /**
   * Throws {@link CorruptedFrameException} if {@code b} is false.
   * @param b the expression to check
   * @param message the message to include in the thrown {@link CorruptedFrameException}, formatted
   *                like {@link com.google.common.base.Preconditions#checkArgument(boolean)} and
   *                friends
   * @param arg1 the first argument to format the message with
   * @param arg2 the second argument to format the message with
   */
  public static void checkFrame(boolean b, String message, Object arg1, Object arg2) {
    if (!b) {
      throw new CorruptedFrameException(Strings.lenientFormat(message, arg1, arg2));
    }
  }

  /**
   * Throws {@link CorruptedFrameException} if {@code b} is false.
   * @param b the expression to check
   * @param message the message to include in the thrown {@link CorruptedFrameException}, formatted
   *                like {@link com.google.common.base.Preconditions#checkArgument(boolean)} and
   *                friends
   * @param args the arguments to format the message with-
   */
  public static void checkFrame(boolean b, String message, Object... args) {
    if (!b) {
      throw new CorruptedFrameException(Strings.lenientFormat(message, args));
    }
  }
}
