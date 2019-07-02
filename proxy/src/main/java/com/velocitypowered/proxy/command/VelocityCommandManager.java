package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class VelocityCommandManager implements CommandManager {

  private final Map<String, RawCommand> commands = new HashMap<>();

  @Override
  @Deprecated
  public void register(final Command command, final String... aliases) {
    Preconditions.checkArgument(aliases.length > 0, "no aliases provided");
    register(aliases[0], command, Arrays.copyOfRange(aliases, 1, aliases.length));
  }

  @Override
  public void register(String alias, Command command, String... otherAliases) {
    Preconditions.checkNotNull(alias, "alias");
    Preconditions.checkNotNull(otherAliases, "otherAliases");
    Preconditions.checkNotNull(command, "executor");

    RawCommand rawCmd = RegularCommandWrapper.wrap(command);
    this.commands.put(alias.toLowerCase(Locale.ENGLISH), rawCmd);

    for (int i = 0, length = otherAliases.length; i < length; i++) {
      final String alias1 = otherAliases[i];
      Preconditions.checkNotNull(alias1, "alias at index %s", i + 1);
      this.commands.put(alias1.toLowerCase(Locale.ENGLISH), rawCmd);
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

    String alias = cmdLine;
    String args = "";
    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace != -1) {
      alias = cmdLine.substring(0, firstSpace);
      args = cmdLine.substring(firstSpace).trim();
    }
    RawCommand command = commands.get(alias.toLowerCase(Locale.ENGLISH));

    try {
      if (!command.hasPermission(source, args)) {
        return false;
      }
      command.execute(source, args);
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

    System.out.println("\"" + cmdLine + "\"");

    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace == -1) {
      // Offer to fill in commands.
      ImmutableList.Builder<String> availableCommands = ImmutableList.builder();
      for (Map.Entry<String, RawCommand> entry : commands.entrySet()) {
        if (entry.getKey().regionMatches(true, 0, cmdLine, 0, cmdLine.length())
            && entry.getValue().hasPermission(source, new String[0])) {
          availableCommands.add("/" + entry.getKey());
        }
      }
      return availableCommands.build();
    }

    String alias = cmdLine.substring(0, firstSpace);
    String args = cmdLine.substring(firstSpace).trim();
    RawCommand command = commands.get(alias.toLowerCase(Locale.ENGLISH));
    if (command == null) {
      // No such command, so we can't offer any tab complete suggestions.
      return ImmutableList.of();
    }

    try {
      if (!command.hasPermission(source, args)) {
        return ImmutableList.of();
      }
      return ImmutableList.copyOf(command.suggest(source, args));
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to invoke suggestions for command " + cmdLine + " for " + source, e);
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

    String alias = cmdLine;
    String args = "";
    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace != -1) {
      alias = cmdLine.substring(0, firstSpace);
      args = cmdLine.substring(firstSpace).trim();
    }
    RawCommand command = commands.get(alias.toLowerCase(Locale.ENGLISH));

    try {
      return command.hasPermission(source, args);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to invoke suggestions for command " + alias + " for " + source, e);
    }
  }

  private static class RegularCommandWrapper implements RawCommand {

    private final Command delegate;

    private RegularCommandWrapper(Command delegate) {
      this.delegate = delegate;
    }

    @Override
    public void execute(CommandSource source, String commandLine) {
      delegate.execute(source, commandLine.split(" ", -1));
    }

    @Override
    public List<String> suggest(CommandSource source, String currentLine) {
      return delegate.suggest(source, currentLine.split(" ", -1));
    }

    @Override
    public boolean hasPermission(CommandSource source, String commandLine) {
      return delegate.hasPermission(source, commandLine.split(" ", -1));
    }

    static RawCommand wrap(Command command) {
      if (command instanceof RawCommand) {
        return (RawCommand) command;
      }
      return new RegularCommandWrapper(command);
    }
  }
}
