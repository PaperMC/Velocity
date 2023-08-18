/*
 * Copyright (C) 2018-2020 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

/**
 * A specialized sub-interface of {@code Command} which indicates the proxy should pass
 * the command and its arguments directly without further processing.
 * This is useful for bolting on external command frameworks to Velocity.
 */
public interface RawCommand extends InvocableCommand<RawCommand.Invocation> {

  /**
   * Contains the invocation data for a raw command.
   */
  interface Invocation extends CommandInvocation<String> {

    /**
     * Returns the used alias to execute the command.
     *
     * @return the used command alias
     */
    String alias();
  }
}
