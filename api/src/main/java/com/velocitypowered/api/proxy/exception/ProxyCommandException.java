/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.exception;

import com.velocitypowered.api.command.CommandSource;

public class ProxyCommandException extends ProxyException {

  private final CommandSource commandSource;
  private final String command;

  /**
   * Constructor for a ProxyCommandException.
   */
  public ProxyCommandException(String message, Throwable cause,
                               CommandSource commandSource, String command) {
    super(message, cause);
    this.commandSource = commandSource;
    this.command = command;
  }

  protected ProxyCommandException(String message, Throwable cause, boolean enableSuppression,
                                  boolean writableStackTrace, CommandSource commandSource, String command) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.commandSource = commandSource;
    this.command = command;
  }

  /**
   * Constructor for a ProxyCommandException.
   */
  public ProxyCommandException(Throwable cause, CommandSource commandSource, String command) {
    super(cause);
    this.commandSource = commandSource;
    this.command = command;
  }

  public CommandSource getCommandSource() {
    return commandSource;
  }

  public String getCommand() {
    return command;
  }
}
