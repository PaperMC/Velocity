/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

public class ProxyException extends Exception {

  public ProxyException(String message) {
    super(message);
  }

  public ProxyException(String message, Throwable cause) {
    super(message, cause);
  }

  protected ProxyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public ProxyException(Throwable cause) {
    super(cause);
  }
}
