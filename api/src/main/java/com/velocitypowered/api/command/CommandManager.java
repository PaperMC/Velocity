package com.velocitypowered.api.command;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the registration and execution of commands.
 */
public interface CommandManager {

  /**
   * Returns a builder to register a {@link BrigadierCommand}.
   *
   * @return a Brigadier command builder
   */
  BrigadierCommand.Builder brigadierBuilder();

  /**
   * Registers the specified command with the specified aliases.
   *
   * @param command the command to register
   * @param aliases the command aliases
   *
   * @throws IllegalArgumentException if one of the given aliases is already registered
   * @deprecated This method requires at least one alias, but this is only enforced at runtime.
   *             Prefer {@link #register(String, Command, String...)}
   */
  @Deprecated
  void register(Command command, String... aliases);

  /**
   * Registers the specified command with the specified aliases.
   *
   * @param alias the first command alias
   * @param command the command to register
   * @param otherAliases additional aliases
   * @throws IllegalArgumentException if one of the given aliases is already registered
   */
  void register(String alias, Command command, String... otherAliases);

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
