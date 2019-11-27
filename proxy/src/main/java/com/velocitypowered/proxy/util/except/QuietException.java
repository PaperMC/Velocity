package com.velocitypowered.proxy.util.except;

/**
 * A special-purpose exception thrown when we want to indicate an error condition but do not want
 * to see a large stack trace in logs.
 */
public class QuietException extends RuntimeException {

  public QuietException(String message) {
    super(message);
  }

  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }
}
