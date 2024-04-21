/*
 * Copyright (C) 2020-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

/**
 * The result of a command invocation attempt.
 */
public enum CommandResult {
  /**
   * The command was successfully executed by the proxy.
   */
  EXECUTED,
  /**
   * The command was forwarded to the backend server.
   * The command may be successfully executed or not
   */
  FORWARDED,
  /**
   * The provided command input contained syntax errors.
   */
  SYNTAX_ERROR,
  /**
   * An unexpected exception occurred while executing the command in the proxy.
   */
  EXCEPTION
}
