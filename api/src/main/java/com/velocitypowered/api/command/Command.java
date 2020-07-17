package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import java.util.List;

/**
 * Represents a command that can be executed by a {@link CommandSource},
 * such as a {@link Player} or the console.
 */
public interface Command<C extends CommandInvocation> {

  /**
   * Executes the command for the specified invocation.
   *
   * @param invocation the invocation data
   */
  void execute(C invocation);

  /**
   * Provides tab complete suggestions for the specified invocation.
   *
   * @param context the execution context
   * @return the tab complete suggestions
   */
  default List<String> suggest(final C context) {
    return ImmutableList.of();
  }

  /**
   * Test to check if the source has permission to use this command with
   * the provided arguments.
   *
   * <p>If the method returns {@code false}, the handling is forwarded onto
   * the players current server.
   *
   * @param context the execution context
   * @return {@code true} if the source has permission
   */
  default boolean hasPermission(final C context) {
    return true;
  }

  /**
   * Provides a fluent interface to register a command.
   *
   * @param <T> the type of the built command
   * @param <B> the type of this builder
   */
  // TODO Is this useful to API users? Velocity only uses it for building Brigadier commands
  // See https://community.oracle.com/blogs/emcmanus/2010/10/24/using-builder-pattern-subclasses
  interface Builder<T, B extends Builder<T, B>> {

    /**
     * Specifies additional aliases that can be used to execute the command.
     *
     * @param aliases the command aliases
     * @return this builder, for chaining
     */
    B aliases(String... aliases);
  }
}
