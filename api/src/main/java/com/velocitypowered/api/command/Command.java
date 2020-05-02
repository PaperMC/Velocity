package com.velocitypowered.api.command;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Represents a command that can be executed by a {@link CommandSource}, such as a {@link
 * com.velocitypowered.api.proxy.Player} or the console.
 */
public interface Command {

  /**
   * Executes the command for the specified {@link CommandSource}.
   *
   * @param source the source of this command
   * @param alias the alias used to execute this command
   * @param args the arguments for this command
   */
  void execute(CommandSource source, String alias, String @NonNull [] args);

  /**
   * Provides tab complete suggestions for a command for a specified {@link CommandSource}.
   *
   * @param source the source to run the command for
   * @param currentArgs the current, partial arguments for this command
   * @return tab complete suggestions
   */
  default List<String> suggest(CommandSource source, String @NonNull [] currentArgs) {
    return ImmutableList.of();
  }

  /**
   * Tests to check if the {@code source} has permission to use this command with the provided
   * {@code args}.
   *
   * <p>If this method returns false, the handling will be forwarded onto
   * the players current server.</p>
   *
   * @param source the source of the command
   * @param args the arguments for this command
   * @return whether the source has permission
   */
  default boolean hasPermission(CommandSource source, String @NonNull [] args) {
    return true;
  }
}
