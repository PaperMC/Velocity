package com.velocitypowered.api.command;

import java.util.function.Consumer;

/**
 * A legacy 1.12-style command.
 */
public interface LegacyCommand extends Command<LegacyCommandExecutionContext> {

  /**
   * Provides a fluent interface to register a legacy command.
   */
  interface Builder extends Command.Builder<LegacyCommand, Builder> {

    /**
     * Specifies additional aliases that can be used to execute the command.
     *
     * @param aliases the command aliases
     * @return this builder, for chaining
     */
    Builder aliases(String... aliases);

    /**
     * Registers the command with the specified execution handler.
     *
     * @param onExecute the execution handler
     * @return the registered command
     */
    LegacyCommand register(Consumer<LegacyCommandExecutionContext> onExecute);
  }
}
