package com.velocitypowered.api.event.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player runs a command!.
 */
public final class CommandEvent implements ResultedEvent<CommandEvent.CommandResult> {

  private final Player player;
  private final String command;
  private final String[] args;
  private CommandResult result;

  /**
   * Constructs a CommandEvent.
   * @param player the player sending the command
   * @param command the command being sent
   * @param args the arguments being sent
   */
  public CommandEvent(Player player, String command, String[] args) {
    this.player = Preconditions.checkNotNull(player, "player");
    this.command = Preconditions.checkNotNull(command, "command");
    this.args = Preconditions.checkNotNull(args, "args");
    this.result = CommandResult.allowed();
  }

  public Player getPlayer() {
    return player;
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
    return "CommandEvent{"
        + "player=" + player
        + ", command=" + command
        + ", args=" + args
        + ", result=" + result
        + '}';
  }

  /**
   * Represents the result of the {@link CommandEvent}.
   */
  public static final class CommandResult implements ResultedEvent.Result {

    private static final CommandResult ALLOWED = new CommandResult(true, null, null);
    private static final CommandResult DENIED = new CommandResult(false, null, null);

    private @Nullable String command;
    private @Nullable String[] args;
    private final boolean status;

    private CommandResult(boolean status, @Nullable String command, @Nullable String[] args) {
      this.status = status;
      this.command = command;
      this.args = args;
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
     * @return the allowed result
     */
    public static CommandResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the command from being sent.
     * @return the denied result
     */
    public static CommandResult denied() {
      return DENIED;
    }

    public Optional<String> getCommand() {
      return Optional.ofNullable(command);
    }

    public Optional<String[]> getCommandArgs() {
      return Optional.ofNullable(args);
    }

    /**
     * Allows the command to be sent, but silently replaced with another.
     * @param command the command to use instead
     * @return a result with a new command
     */
    public static CommandResult command(@NonNull String command, @NonNull String[] args) {
      Preconditions.checkNotNull(command, "command");
      Preconditions.checkNotNull(args, "args");
      return new CommandResult(true, command, args);
    }
  }


}
