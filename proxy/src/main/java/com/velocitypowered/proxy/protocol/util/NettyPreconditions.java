package com.velocitypowered.proxy.protocol.util;

import com.google.common.base.Strings;
import com.velocitypowered.proxy.protocol.netty.MinecraftDecoder;
import com.velocitypowered.proxy.util.except.QuietException;
import io.netty.handler.codec.CorruptedFrameException;

/**
 * Extends {@link com.google.common.base.Preconditions} for Netty's {@link CorruptedFrameException}.
 */
public class NettyPreconditions {
  private static final QuietException BAD = new QuietException(
      "Invalid packet received. Launch Velocity with -Dvelocity.packet-decode-logging=true "
          + "to see more.");

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
      throw MinecraftDecoder.DEBUG ? new CorruptedFrameException(message) : BAD;
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
      if (MinecraftDecoder.DEBUG) {
        throw new CorruptedFrameException(Strings.lenientFormat(message, arg1));
      } else {
        throw BAD;
      }
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
      if (MinecraftDecoder.DEBUG) {
        throw new CorruptedFrameException(Strings.lenientFormat(message, arg1, arg2));
      } else {
        throw BAD;
      }
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
      if (MinecraftDecoder.DEBUG) {
        throw new CorruptedFrameException(Strings.lenientFormat(message, args));
      } else {
        throw BAD;
      }
    }
  }
}
