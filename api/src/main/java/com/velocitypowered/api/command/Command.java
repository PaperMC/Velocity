package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.Player;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a command that can be executed by a {@link CommandSource}, such as
 * a {@link Player} or the console.
 */
public interface Command<I extends CommandInvocation> {

  /**
   * Executes the command for the specified invocation.
   *
   * @param invocation the invocation context
   */
  void execute(I invocation);

  /**
   * Provides tab complete suggestions for the specified invocation.
   *
   * @param invocation the invocation context
   * @return the tab complete suggestions
   */
  default List<String> suggest(final I invocation) {
    return ImmutableList.of();
  }

  /**
   * Provides tab complete suggestions for the specified invocation.
   *
   * @param invocation the invocation context
   * @return the tab complete suggestions
   * @implSpec defaults to wrapping the value returned by {@link #suggest(CommandInvocation)}
   */
  default CompletableFuture<List<String>> suggestAsync(final I invocation) {
    return CompletableFuture.completedFuture(suggest(invocation));
  }

  /**
   * Tests to check if the source has permission to perform the specified invocation.
   *
   * <p>If the method returns {@code false}, the handling is forwarded onto
   * the players current server.
   *
   * @param invocation the invocation context
   * @return {@code true} if the source has permission
   */
  default boolean hasPermission(final I invocation) {
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
