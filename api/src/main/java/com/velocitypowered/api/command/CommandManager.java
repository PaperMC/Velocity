package com.velocitypowered.api.command;

import java.util.concurrent.CompletableFuture;

/**
 * Represents an interface to register a command executor with the proxy.
 */
public interface CommandManager {

  /**
   * Registers the specified command with the manager with the specified aliases.
   *
   * @param command the command to register
   * @param aliases the alias to use
   *
   * @deprecated This method requires at least one alias, but this is only enforced at runtime.
   *             Prefer {@link #register(String, Command, String...)} instead.
   */
  @Deprecated
  void register(Command command, String... aliases);

  /**
   * Registers the specified command with the manager with the specified aliases.
   *
   * @param alias the first alias to register
   * @param command the command to register
   * @param otherAliases the other aliases to use
   */
  void register(String alias, Command command, String... otherAliases);

  /**
   * Unregisters a command.
   *
   * @param alias the command alias to unregister
   */
  void unregister(String alias);

  /**
   * Calls CommandExecuteEvent and attempts to execute a command using the specified {@code cmdLine}
   * in a blocking fashion.
   *
   * @param source the command's source
   * @param cmdLine the command to run
   * @return true if the command was found and executed, false if it was not
   * 
   * @deprecated This method will block current thread during event call and command execution.
   *             Prefer {@link #executeAsync(CommandSource, String)} instead.
   */
  @Deprecated
  boolean execute(CommandSource source, String cmdLine);

  /**
   * Attempts to execute a command using the specified {@code cmdLine} in a blocking fashion without
   * calling CommandExecuteEvent.
   *
   * @param source the command's source
   * @param cmdLine the command to run
   * @return true if the command was found and executed, false if it was not
   * 
   * @deprecated This method will block current thread during event and command execution.
   *             Prefer {@link #executeImmediatelyAsync(CommandSource, String)} instead.
   */
  @Deprecated
  boolean executeImmediately(CommandSource source, String cmdLine);

  /**
   * Calls CommandExecuteEvent and attempts to execute a command from the specified {@code cmdLine}
   * async.
   *
   * @param source the command's source
   * @param cmdLine the command to run
   * @return A future that will be completed with the result of the command execution.
   *         Can be completed exceptionally if exception was thrown during execution.
   */
  CompletableFuture<Boolean> executeAsync(CommandSource source, String cmdLine);

  /**
   * Attempts to execute a command from the specified {@code cmdLine} async
   * without calling CommandExecuteEvent.
   *
   * @param source the command's source
   * @param cmdLine the command to run
   * @return A future that will be completed with the result of the command execution.
   *         Can be completed exceptionally if exception was thrown during execution.
   */
  CompletableFuture<Boolean> executeImmediatelyAsync(CommandSource source, String cmdLine);
}
