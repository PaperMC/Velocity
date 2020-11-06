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

  /**
   * Returns the alias used to invoke the command.
   *
   * @return the command used to invoke the command.
   */
  String alias();
}
