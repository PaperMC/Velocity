package com.velocitypowered.proxy.util.except;

/**
 * A special-purpose exception thrown when we want to indicate an error but do not want
 * to see a large stack trace in logs.
 */
public class QuietRuntimeException extends RuntimeException {

  public QuietRuntimeException(String message) {
    super(message);
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
