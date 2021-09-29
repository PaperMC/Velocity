/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

public class ProxyInternalException extends ProxyException {

  public ProxyInternalException(String message) {
    super(message);
  }

  public ProxyInternalException(String message, Throwable cause) {
    super(message, cause);
  }

  protected ProxyInternalException(String message, Throwable cause,
                                   boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

  public ProxyInternalException(Throwable cause) {
    super(cause);
  }
}
