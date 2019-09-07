package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A specialized sub-interface of {@code Command} which indicates that the proxy should pass a
 * raw command to the command. This is useful for bolting on external command frameworks to
 * Velocity.
 */
public interface RawCommand extends Command {
  /**
   * Executes the command for the specified {@link CommandSource}.
   *
   * @param source the source of this command
   * @param commandLine the full command line after the command name
   */
  void execute(CommandSource source, String commandLine);

  default void execute(CommandSource source, String @NonNull [] args) {
    execute(source, String.join(" ", args));
  }

  /**
   * Provides tab complete suggestions for a command for a specified {@link CommandSource}.
   *
   * @param source the source to run the command for
   * @param currentLine the current, partial command line for this command
   * @return tab complete suggestions
   */
  default List<String> suggest(CommandSource source, String currentLine) {
    return ImmutableList.of();
  }

  @Override
  default List<String> suggest(CommandSource source, String @NonNull [] currentArgs) {
    return suggest(source, String.join(" ", currentArgs));
  }

  @Override
  default boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return hasPermission(source, String.join(" ", args));
  }

  /**
   * Tests to check if the {@code source} has permission to use this command with the provided
   * {@code args}.
   *
   * <p>If this method returns false, the handling will be forwarded onto
   * the players current server.</p>
   *
   * @param source the source of the command
   * @param commandLine the arguments for this command
   * @return whether the source has permission
   */
  default boolean hasPermission(CommandSource source, String commandLine) {
    return true;
  }
}
