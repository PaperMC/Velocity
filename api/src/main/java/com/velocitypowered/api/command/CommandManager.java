package com.velocitypowered.api.command;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Registers commands executors for the proxy.
 */
public interface CommandManager {

  /**
   * Returns a builder to register a {@link LegacyCommand}.
   *
   * @return a legacy command builder
   */
  LegacyCommand.Builder legacyBuilder();

  /**
   * Returns a builder to register a {@link BrigadierCommand}.
   *
   * @return a Brigadier command builder
   */
  BrigadierCommand.Builder brigadierBuilder();

  /**
   * Unregisters the specified command, if registered.
   *
   * @param command the command to unregister
   */
  void unregister(Command<?> command);

  /**
   * Unregisters the specified command alias from the manager, if registered.
   *
   * @param alias the command alias to unregister
   */
  void unregister(String alias);

  /**
   * Attempts to asynchronously execute a command from the given {@code cmdLine}.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to run
   * @return a future that may be completed with the result of the command execution.
   *         Can be completed exceptionally if an exception is thrown during execution.
   */
  // TODO Move to CompletableFuture<CommandResult>?
  CompletableFuture<Boolean> execute(CommandSource source, String cmdLine);

  /**
   * Attempts to asynchronously execute a command from the given {@code cmdLine}
   * without firing a {@link CommandExecuteEvent}.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to run
   * @return a future that may be completed with the result of the command execution.
   *         Can be completed exceptionally if an exception is thrown during execution.
   */
  CompletableFuture<Boolean> executeImmediately(CommandSource source, String cmdLine);
}
