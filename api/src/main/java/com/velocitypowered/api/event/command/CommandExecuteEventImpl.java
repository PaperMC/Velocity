/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;

/**
 * This event is fired when someone executing command.
 */
public final class CommandExecuteEventImpl implements CommandExecuteEvent {

  private final CommandSource commandSource;
  private final String command;
  private CommandResult result;

  /**
   * Constructs a CommandExecuteEvent.
   * @param commandSource the source executing the command
   * @param command the command being executed without first slash
   */
  public CommandExecuteEventImpl(CommandSource commandSource, String command) {
    this.commandSource = Preconditions.checkNotNull(commandSource, "commandSource");
    this.command = Preconditions.checkNotNull(command, "command");
    this.result = CommandResult.allowed();
  }

  @Override
  public CommandSource source() {
    return commandSource;
  }

  /**
   * Gets the original command being executed without first slash.
   * @return the original command being executed
   */
  @Override
  public String rawCommand() {
    return command;
  }

  @Override
  public CommandResult result() {
    return result;
  }

  @Override
  public void setResult(CommandResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "CommandExecuteEvent{"
        + "commandSource=" + commandSource
        + ", command=" + command
        + ", result=" + result
        + '}';
  }

}
