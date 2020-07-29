package com.velocitypowered.proxy.util.except;

import io.netty.handler.codec.DecoderException;

/**
 * A special-purpose exception thrown when we want to indicate an error decoding but do not want
 * to see a large stack trace in logs.
 */
public class QuietDecoderException extends DecoderException {

  public QuietDecoderException(String message) {
    super(message);
  }

  @Override
  public Throwable fillInStackTrace() {
    return this;
  }

  @Override
  public Throwable initCause(Throwable cause) {
    return this;
  }
}
