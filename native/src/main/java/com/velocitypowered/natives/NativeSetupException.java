package com.velocitypowered.natives;

public class NativeSetupException extends RuntimeException {

  public NativeSetupException() {
  }

  public NativeSetupException(String message) {
    super(message);
  }

  public NativeSetupException(String message, Throwable cause) {
    super(message, cause);
  }

  public NativeSetupException(Throwable cause) {
    super(cause);
  }

  public NativeSetupException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
