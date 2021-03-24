/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.velocitypowered.api.event.command.CommandExecuteEvent;
import java.util.concurrent.CompletableFuture;

/**
 * Handles the registration and execution of commands.
 */
public interface CommandManager {

  /**
   * Returns a builder to create a {@link CommandMeta} with
   * the given alias.
   *
   * @param alias the first command alias
   * @return a {@link CommandMeta} builder
   */
  CommandMeta.Builder metaBuilder(String alias);

  /**
   * Returns a builder to create a {@link CommandMeta} for
   * the given Brigadier command.
   *
   * @param command the command
   * @return a {@link CommandMeta} builder
   */
  CommandMeta.Builder metaBuilder(BrigadierCommand command);

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
   * Registers the specified Brigadier command.
   *
   * @param command the command to register
   * @throws IllegalArgumentException if the node alias is already registered
   */
  void register(BrigadierCommand command);

  /**
   * Registers the specified command with the given metadata.
   *
   * @param meta the command metadata
   * @param command the command to register
   * @throws IllegalArgumentException if one of the given aliases is already registered
   */
  void register(CommandMeta meta, Command command);

  /**
   * Unregisters the specified command alias from the manager, if registered.
   *
   * @param alias the command alias to unregister
   */
  void unregister(String alias);

  /**
   * Attempts to execute a command from the given {@code cmdLine} in
   * a blocking fashion.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to run
   * @return {@code true} if the command was found and executed
   * @deprecated this method blocks the current thread during the event call and
   *             the command execution. Prefer {@link #executeAsync(CommandSource, String)}
   *             instead.
   */
  @Deprecated
  boolean execute(CommandSource source, String cmdLine);

  /**
   * Attempts to execute a command from the given {@code cmdLine} without
   * firing a {@link CommandExecuteEvent} in a blocking fashion.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to run
   * @return {@code true} if the command was found and executed
   * @deprecated this methods blocks the current thread during the command execution.
   *             Prefer {@link #executeImmediatelyAsync(CommandSource, String)} instead.
   */
  @Deprecated
  boolean executeImmediately(CommandSource source, String cmdLine);

  /**
   * Attempts to asynchronously execute a command from the given {@code cmdLine}.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to run
   * @return a future that may be completed with the result of the command execution.
   *         Can be completed exceptionally if an exception is thrown during execution.
   */
  CompletableFuture<Boolean> executeAsync(CommandSource source, String cmdLine);

  /**
   * Attempts to asynchronously execute a command from the given {@code cmdLine}
   * without firing a {@link CommandExecuteEvent}.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to run
   * @return a future that may be completed with the result of the command execution.
   *         Can be completed exceptionally if an exception is thrown during execution.
   */
  CompletableFuture<Boolean> executeImmediatelyAsync(CommandSource source, String cmdLine);

  /**
   * Returns whether the given alias is registered on this manager.
   *
   * @param alias the command alias to check
   * @return {@code true} if the alias is registered
   */
  boolean hasCommand(String alias);
}
