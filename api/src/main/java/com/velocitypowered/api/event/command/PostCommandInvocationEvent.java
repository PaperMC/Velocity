/*
 * Copyright (C) 2020-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandResult;
import com.velocitypowered.api.command.CommandSource;

/**
 * This event is fired when velocity executed a command. This event is called after the event
 * is handled.
 *
 * <p>Commands can be cancelled or forwarded to backend servers in {@link CommandExecuteEvent}.
 * This will prevent firing this event.</p>
 */
public final class PostCommandInvocationEvent {

  private final CommandSource commandSource;
  private final String command;
  private final CommandResult result;

  /**
   * Constructs a PostCommandInvocationEvent.
   *
   * @param commandSource the source executing the command
   * @param command the command being executed without first slash
   * @param result the result of this command
   */
  public PostCommandInvocationEvent(CommandSource commandSource, String command,
                                    CommandResult result) {
    this.commandSource = Preconditions.checkNotNull(commandSource, "commandSource");
    this.command = Preconditions.checkNotNull(command, "command");
    this.result = Preconditions.checkNotNull(result, "result");
  }

  /**
   * Get the source of this executed command.
   *
   * @return the source
   */
  public CommandSource getCommandSource() {
    return commandSource;
  }

  /**
   * Gets the original command line executed without the first slash.
   *
   * @return the original command
   */
  public String getCommand() {
    return command;
  }

  /**
   * Returns the result of the command execution.
   *
   * @return the execution result
   */
  public CommandResult getResult() {
    return result;
  }

  @Override
  public String toString() {
    return "PostCommandInvocationEvent{"
            + "commandSource=" + commandSource
            + ", command=" + command
            + '}';
  }
}
