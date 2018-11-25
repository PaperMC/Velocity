package com.velocitypowered.api.event.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.ResultedEvent;

import java.util.Arrays;

/**
 * This event is fired when a player runs a command.
 */
public final class CommandExecuteEvent implements ResultedEvent<CommandExecuteEvent.CommandResult> {

  private final CommandSource source;
  private final String command;
  private final String[] args;
  private CommandResult result;

  /**
   * Constructs a CommandExecuteEvent.
   *
   * @param source  the player sending the command
   * @param command the command being sent
   * @param args    the arguments being sent
   */
  public CommandExecuteEvent(CommandSource source, String command, String[] args) {
    this.source = Preconditions.checkNotNull(source, "player");
    this.command = Preconditions.checkNotNull(command, "command");
    this.args = Preconditions.checkNotNull(args, "args");
    this.result = CommandResult.allowed();
  }

  public CommandSource getSource() {
    return source;
  }

  public String getCommand() {
    return command;
  }

  public String[] getCommandArgs() {
    return args;
  }

  @Override
  public CommandResult getResult() {
    return result;
  }

  @Override
  public void setResult(CommandResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "CommandExecuteEvent{"
            + "source=" + source
            + ", command=" + command
            + ", args=" + Arrays.toString(args)
            + ", result=" + result
            + '}';
  }

  /**
   * Represents the result of the {@link CommandExecuteEvent}.
   */
  public static final class CommandResult implements ResultedEvent.Result {

    private static final CommandResult ALLOWED = new CommandResult(true);
    private static final CommandResult DENIED = new CommandResult(false);

    private final boolean status;

    private CommandResult(boolean status) {
      this.status = status;
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
     * Prevents the command from being sent.
     *
     * @return the denied result
     */
    public static CommandResult denied() {
      return DENIED;
    }

  }
}


