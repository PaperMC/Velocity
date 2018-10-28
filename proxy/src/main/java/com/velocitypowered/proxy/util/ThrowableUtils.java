package com.velocitypowered.proxy.util;

public class ThrowableUtils {
  private ThrowableUtils() {
    throw new AssertionError();
  }

  public static String briefDescription(Throwable throwable) {
    return throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
  }
}
