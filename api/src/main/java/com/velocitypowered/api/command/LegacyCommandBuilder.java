package com.velocitypowered.api.command;

import java.util.function.Consumer;

/**
 * Provides a fluent interface to register a legacy 1.12-style command.
 */
public interface LegacyCommandBuilder extends Command.Builder<Command<LegacyCommandExecutionContext>, LegacyCommandBuilder> {

  /**
   * Registers the command with the specified execution handler.
   *
   * @param onExecute the execution handler
   * @return the registered command
   */
  Command<LegacyCommandExecutionContext> register(Consumer<LegacyCommandExecutionContext> onExecute);
}
