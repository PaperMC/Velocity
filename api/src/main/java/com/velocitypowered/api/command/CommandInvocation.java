/*
 * Copyright (C) 2020-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

/**
 * Provides information related to the possible execution of a {@link Command}.
 *
 * @param <T> the type of the arguments
 */
public interface CommandInvocation<T> {

  /**
   * Returns the source to execute the command for.
   *
   * @return the command source
   */
  CommandSource source();

  /**
   * Returns the arguments after the command alias.
   *
   * @return the command arguments
   */
  T arguments();
}
