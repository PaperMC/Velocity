/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

import com.velocitypowered.api.command.CommandSource;

public class ProxyTabCompleteException extends ProxyCommandException {

  public ProxyTabCompleteException(String message, Throwable cause, CommandSource commandSource, String command) {
    super(message, cause, commandSource, command);
  }

  protected ProxyTabCompleteException(String message, Throwable cause, boolean enableSuppression,
                                      boolean writableStackTrace, CommandSource commandSource, String command) {
    super(message, cause, enableSuppression, writableStackTrace, commandSource, command);
  }

  public ProxyTabCompleteException(Throwable cause, CommandSource commandSource, String command) {
    super(cause, commandSource, command);
  }
}
