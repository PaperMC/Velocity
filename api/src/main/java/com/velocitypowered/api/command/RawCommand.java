/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A specialized sub-interface of {@code Command} which indicates the proxy should pass
 * the command and its arguments directly without further processing.
 * This is useful for bolting on external command frameworks to Velocity.
 */
public interface RawCommand extends InvocableCommand<RawCommand.Invocation> {

  /**
   * Executes the command for the specified source.
   *
   * @param source the source to execute the command for
   * @param cmdLine the arguments for the command
   * @deprecated see {@link Command}
   */
  @Deprecated
  default void execute(final CommandSource source, final String cmdLine) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  default void execute(final CommandSource source, final String @NonNull [] args) {
    execute(source, String.join(" ", args));
  }

  @Override
  default void execute(Invocation invocation) {
    // Guarantees ABI compatibility
  }

  /**
   * Provides tab complete suggestions for the specified source.
   *
   * @param source the source to execute the command for
   * @param currentArgs the partial arguments for the command
   * @return the tab complete suggestions
   * @deprecated see {@link Command}
   */
  @Deprecated
  default CompletableFuture<List<String>> suggest(final CommandSource source,
                                                  final String currentArgs) {
    // This method even has an inconsistent return type
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  default List<String> suggest(final CommandSource source, final String @NonNull [] currentArgs) {
    return suggestAsync(source, currentArgs).join();
  }

  @Deprecated
  @Override
  default CompletableFuture<List<String>> suggestAsync(final CommandSource source,
                                                       final String @NonNull [] currentArgs) {
    return suggest(source, String.join(" ", currentArgs));
  }

  /**
   * Tests to check if the source has permission to perform the command with
   * the provided arguments.
   *
   * @param source the source to execute the command for
   * @param cmdLine the arguments for the command
   * @return {@code true} if the source has permission
   * @deprecated see {@link Command}
   */
  @Deprecated
  default boolean hasPermission(final CommandSource source, final String cmdLine) {
    throw new UnsupportedOperationException();
  }

  @Deprecated
  @Override
  default boolean hasPermission(final CommandSource source, final String @NonNull [] args) {
    return hasPermission(source, String.join(" ", args));
  }

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
