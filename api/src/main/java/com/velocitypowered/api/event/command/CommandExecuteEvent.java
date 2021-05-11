/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when someone executes a command.
 */
public interface CommandExecuteEvent extends ResultedEvent<CommandResult> {

  CommandSource source();

  /**
   * Gets the original command being executed without the first slash.
   * @return the original command being executed
   */
  String rawCommand();

  final class CommandResult implements ResultedEvent.Result {

    private static final CommandResult ALLOWED = new CommandResult(true, false, null);
    private static final CommandResult DENIED = new CommandResult(false, false, null);
    private static final CommandResult FORWARD_TO_SERVER = new CommandResult(false, true, null);

    private @Nullable String command;
    private final boolean status;
    private final boolean forward;

    private CommandResult(boolean status, boolean forward, @Nullable String command) {
      this.status = status;
      this.forward = forward;
      this.command = command;
    }

    public Optional<String> modifiedCommand() {
      return Optional.ofNullable(command);
    }

    public boolean isForwardToServer() {
      return forward;
    }

    @Override
    public boolean isAllowed() {
      return status;
    }

    @Override
    public String toString() {
      return status ? "allowed" : "denied";
    }

    /**
     * Allows the command to be sent, without modification.
     *
     * @return the allowed result
     */
    public static CommandResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the command from being executed.
     *
     * @return the denied result
     */
    public static CommandResult denied() {
      return DENIED;
    }

    /**
     * Prevents the command from being executed, but forward command to server.
     *
     * @return the forward result
     */
    public static CommandResult forwardToServer() {
      return FORWARD_TO_SERVER;
    }

    /**
     * Prevents the command from being executed on proxy, but forward command to server.
     *
     * @param newCommand the command without first slash to use instead
     * @return a result with a new command being forwarded to server
     */
    public static CommandResult forwardToServer(@NonNull String newCommand) {
      Preconditions.checkNotNull(newCommand, "newCommand");
      return new CommandResult(false, true, newCommand);
    }

    /**
     * Allows the command to be executed, but silently replaced old command with another.
     *
     * @param newCommand the command to use instead without first slash
     * @return a result with a new command
     */
    public static CommandResult command(@NonNull String newCommand) {
      Preconditions.checkNotNull(newCommand, "newCommand");
      return new CommandResult(true, false, newCommand);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      CommandResult that = (CommandResult) o;
      return status == that.status && forward == that.forward
          && Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
      return Objects.hash(command, status, forward);
    }
  }

}
