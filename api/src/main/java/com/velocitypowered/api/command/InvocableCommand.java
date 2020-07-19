package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// TODO Document
public interface InvocableCommand<I extends CommandInvocation<?>> extends Command {

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
}
