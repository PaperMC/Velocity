package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player types in a chat command.
 */
public final class PlayerCommandPreProcessEvent implements
    ResultedEvent<PlayerCommandPreProcessEvent.CommandResult> {

  private final Player player;
  private final String command;
  private CommandResult result;

  /**
   * Constructs a PlayerCommandPreProcessEvent.
   *
   * @param player  the player sending the message
   * @param command the command being sent
   */
  public PlayerCommandPreProcessEvent(Player player, String command) {
    this.player = player;
    this.command = command;
    this.result = CommandResult.ALLOWED;
  }

  public Player getPlayer() {
    return player;
  }

  public String getCommand() {
    return command;
  }

  @Override
  public CommandResult getResult() {
    return this.result;
  }

  @Override
  public void setResult(CommandResult result) {
    this.result = result;
  }

  /**
   * Represents the result of the {@link PlayerCommandPreProcessEvent}.
   */
  public static final class CommandResult implements ResultedEvent.Result {

    private static final PlayerCommandPreProcessEvent.CommandResult ALLOWED =
        new PlayerCommandPreProcessEvent.CommandResult(true, null);
    private static final PlayerCommandPreProcessEvent.CommandResult DENIED =
        new PlayerCommandPreProcessEvent.CommandResult(false, null);

    private final String command;
    private final boolean status;

    private CommandResult(boolean status, @Nullable String command) {
      this.status = status;
      this.command = command;
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
    public static PlayerCommandPreProcessEvent.CommandResult allowed() {
      return ALLOWED;
    }

    /**
     * Prevents the command from being sent.
     *
     * @return the denied result
     */
    public static PlayerCommandPreProcessEvent.CommandResult denied() {
      return DENIED;
    }

    public Optional<String> getCommand() {
      return Optional.ofNullable(command);
    }

    /**
     * Allows the command to be sent, but silently replaced with another.
     *
     * @param command the command to use instead
     * @return a result with a new command
     */
    public static PlayerCommandPreProcessEvent.CommandResult command(@NonNull String command) {
      Preconditions.checkNotNull(command, "command");
      return new PlayerCommandPreProcessEvent.CommandResult(true, command);
    }
  }
}
