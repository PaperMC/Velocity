/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.exception.ProxyException;

/**
 * This event is fired by the proxy when an exception is thrown
 * in a recoverable section of the server.
 */
public class ProxyExceptionEvent {

  private final ProxyException exception;

  public ProxyExceptionEvent(ProxyException exception) {
    this.exception = Preconditions.checkNotNull(exception, "exception");
  }

  /**
   * Gets the wrapped exception that was thrown.
   *
   * @return Thrown exception
   */
  public ProxyException getException() {
    return exception;
  }

  @Override
  public String toString() {
    return "ProxyExceptionEvent{"
        + "exception=" + exception
        + '}';
  }
}
