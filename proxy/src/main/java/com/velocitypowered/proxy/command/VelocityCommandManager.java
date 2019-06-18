package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.NonNull;

public class VelocityCommandManager implements CommandManager {

  private final Map<String, Command> commands = new HashMap<>();

  @Override
  public void register(final Command command, final String... aliases) {
    Preconditions.checkNotNull(aliases, "aliases");
    Preconditions.checkNotNull(command, "executor");
    for (int i = 0, length = aliases.length; i < length; i++) {
      final String alias = aliases[i];
      Preconditions.checkNotNull(alias, "alias at index %s", i);
      this.commands.put(alias.toLowerCase(Locale.ENGLISH), command);
    }
  }

  @Override
  public void unregister(final String alias) {
    Preconditions.checkNotNull(alias, "name");
    this.commands.remove(alias.toLowerCase(Locale.ENGLISH));
  }

  @Override
  public boolean execute(CommandSource source, String cmdLine) {
    Preconditions.checkNotNull(source, "invoker");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    String[] split = cmdLine.split(" ", -1);
    if (split.length == 0) {
      return false;
    }

    String alias = split[0];
    Command command = commands.get(alias.toLowerCase(Locale.ENGLISH));
    if (command == null) {
      return false;
    }

    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
    try {
      if (command instanceof RawCommand) {
        RawCommand rc = (RawCommand) command;
        int firstSpace = cmdLine.indexOf(' ');
        String line = firstSpace == -1 ? "" : cmdLine.substring(firstSpace + 1);
        if (!rc.hasPermission(source, line)) {
          return false;
        }
        rc.execute(source, line);
      } else {
        if (!command.hasPermission(source, actualArgs)) {
          return false;
        }
        command.execute(source, actualArgs);
      }
      return true;
    } catch (Exception e) {
      throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
    }
  }

  public boolean hasCommand(String command) {
    return commands.containsKey(command);
  }

  public Set<String> getAllRegisteredCommands() {
    return ImmutableSet.copyOf(commands.keySet());
  }

  /**
   * Offer suggestions to fill in the command.
   * @param source the source for the command
   * @param cmdLine the partially completed command
   * @return a {@link List}, possibly empty
   */
  public List<String> offerSuggestions(CommandSource source, String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    String[] split = cmdLine.split(" ", -1);
    if (split.length == 0) {
      // No command available.
      return ImmutableList.of();
    }

    String alias = split[0];
    if (split.length == 1) {
      // Offer to fill in commands.
      ImmutableList.Builder<String> availableCommands = ImmutableList.builder();
      for (Map.Entry<String, Command> entry : commands.entrySet()) {
        if (entry.getKey().regionMatches(true, 0, alias, 0, alias.length())
            && entry.getValue().hasPermission(source, new String[0])) {
          availableCommands.add("/" + entry.getKey());
        }
      }
      return availableCommands.build();
    }

    Command command = commands.get(alias.toLowerCase(Locale.ENGLISH));
    if (command == null) {
      // No such command, so we can't offer any tab complete suggestions.
      return ImmutableList.of();
    }

    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
    try {
      if (command instanceof RawCommand) {
        RawCommand rc = (RawCommand) command;
        int firstSpace = cmdLine.indexOf(' ');
        String line = firstSpace == -1 ? "" : cmdLine.substring(firstSpace + 1);
        if (!rc.hasPermission(source, line)) {
          return ImmutableList.of();
        }
        return ImmutableList.copyOf(rc.suggest(source, line));
      } else {
        if (!command.hasPermission(source, actualArgs)) {
          return ImmutableList.of();
        }
        return ImmutableList.copyOf(command.suggest(source, actualArgs));
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to invoke suggestions for command " + alias + " for " + source, e);
    }
  }

  /**
   * Determines if the {@code source} has permission to run the {@code cmdLine}.
   * @param source the source to check against
   * @param cmdLine the command to run
   * @return {@code true} if the command can be run, otherwise {@code false}
   */
  public boolean hasPermission(CommandSource source, String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    String[] split = cmdLine.split(" ", -1);
    if (split.length == 0) {
      // No command available.
      return false;
    }

    String alias = split[0];
    Command command = commands.get(alias.toLowerCase(Locale.ENGLISH));
    if (command == null) {
      // No such command.
      return false;
    }

    @SuppressWarnings("nullness")
    String[] actualArgs = Arrays.copyOfRange(split, 1, split.length);
    try {
      if (command instanceof RawCommand) {
        RawCommand rc = (RawCommand) command;
        int firstSpace = cmdLine.indexOf(' ');
        String line = firstSpace == -1 ? "" : cmdLine.substring(firstSpace + 1);
        return rc.hasPermission(source, line);
      } else {
        return command.hasPermission(source, actualArgs);
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to invoke suggestions for command " + alias + " for " + source, e);
    }
  }
}
