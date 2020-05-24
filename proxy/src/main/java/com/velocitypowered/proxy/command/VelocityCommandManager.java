package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.plugin.VelocityEventManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class VelocityCommandManager implements CommandManager {

  private final Map<String, RawCommand> commands = new HashMap<>();
  private final VelocityEventManager eventManager;

  public VelocityCommandManager(VelocityEventManager eventManager) {
    this.eventManager = eventManager;
  }

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

  /**
   * Calls CommandExecuteEvent.
   * @param source the command's source
   * @param cmd the command
   * @return CompletableFuture of event
   */
  public CompletableFuture<CommandExecuteEvent> callCommandEvent(CommandSource source, String cmd) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmd, "cmd");
    return eventManager.fire(new CommandExecuteEvent(source, cmd));
  }

  @Override
  public boolean execute(CommandSource source, String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    CommandExecuteEvent event = callCommandEvent(source, cmdLine).join();
    CommandResult commandResult = event.getResult();
    if (commandResult.isForwardToServer() || !commandResult.isAllowed()) {
      return false;
    }
    cmdLine = commandResult.getCommand().orElse(event.getCommand());

    return executeImmediately(source, cmdLine);
  }

  @Override
  public boolean executeImmediately(CommandSource source, String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    String alias = cmdLine;
    String args = "";
    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace != -1) {
      alias = cmdLine.substring(0, firstSpace);
      args = cmdLine.substring(firstSpace);
    }
    RawCommand command = commands.get(alias.toLowerCase(Locale.ENGLISH));
    if (command == null) {
      return false;
    }

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


  @Override
  public CompletableFuture<Boolean> executeAsync(CommandSource source, String cmdLine) {
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    callCommandEvent(source, cmdLine).thenAccept(event -> {
      CommandResult commandResult = event.getResult();
      if (commandResult.isForwardToServer() || !commandResult.isAllowed()) {
        result.complete(false);
      }
      String command = commandResult.getCommand().orElse(event.getCommand());
      try {
        result.complete(executeImmediately(source, command));
      } catch (Exception e) {
        result.completeExceptionally(e);
      }
    });
    return result;
  }

  @Override
  public CompletableFuture<Boolean> executeImmediatelyAsync(CommandSource source, String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");
    CompletableFuture<Boolean> result = new CompletableFuture<>();
    eventManager.getService().execute(() -> {
      try {
        result.complete(executeImmediately(source, cmdLine));
      } catch (Exception e) {
        result.completeExceptionally(e);
      }
    });
    return result;
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
    String args = cmdLine.substring(firstSpace);
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
    if (command == null) {
      return false;
    }

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

    private static String[] split(String line) {
      if (line.isEmpty()) {
        return new String[0];
      }

      String[] trimmed = line.trim().split(" ", -1);
      if (line.endsWith(" ") && !line.trim().isEmpty()) {
        // To work around a 1.13+ specific bug we have to inject a space at the end of the arguments
        trimmed = Arrays.copyOf(trimmed, trimmed.length + 1);
        trimmed[trimmed.length - 1] = "";
      }
      return trimmed;
    }

    @Override
    public void execute(CommandSource source, String commandLine) {
      delegate.execute(source, split(commandLine));
    }

    @Override
    public List<String> suggest(CommandSource source, String currentLine) {
      return delegate.suggest(source, split(currentLine));
    }

    @Override
    public boolean hasPermission(CommandSource source, String commandLine) {
      return delegate.hasPermission(source, split(commandLine));
    }

    static RawCommand wrap(Command command) {
      if (command instanceof RawCommand) {
        return (RawCommand) command;
      }
      return new RegularCommandWrapper(command);
    }
  }
}
