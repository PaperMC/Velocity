package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class VelocityCommandManager implements CommandManager {

  @Deprecated
  static CommandNode<CommandSource> createRedirectNode(final CommandNode<CommandSource> dest,
                                                              final String alias) {
    // Brigadier provides an aliasing system, but it's broken (https://github.com/Mojang/brigadier/issues/46).
    // Additionally, the aliases need to keep working even if the main node is removed.
    return LiteralArgumentBuilder.<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .executes(dest.getCommand())
            .build();
  }

  // TODO Mental notes to write docs later
  // Only BrigadierCommand implementations may use the Brigadier dispatcher.

  // The map contains all registered command aliases. On execution, offer suggestions, and
  // permission checks a CommandInvocation object is created by the factory. This object is then
  // passed to the underlying command, which may use the Brigadier dispatcher.

  private final Map<String, Command<?>> commands = new HashMap<>();

  private final CommandDispatcher<CommandSource> dispatcher = new CommandDispatcher<>();
  private final Map<Class<? extends Command<?>>, CommandInvocationFactory<?>> contextFactories;

  private final VelocityEventManager eventManager;

  public VelocityCommandManager(final VelocityEventManager eventManager) {
    this.eventManager = eventManager;
    this.contextFactories = new HashMap<>(
          ImmutableMap.<Class<? extends Command<?>>, CommandInvocationFactory<?>>builder()
                  .put(RawCommand.class, VelocityRawCommandInvocation.FACTORY)
                  .put(LegacyCommand.class, VelocityLegacyCommandInvocation.FACTORY)
                  .put(BrigadierCommand.class, new VelocityBrigadierCommandInvocation.Factory(dispatcher))
                  .build());
  }

  @Override
  public BrigadierCommand.Builder brigadierBuilder() {
    return null;
  }

  // Registration

  @Override
  public void register(final Command<?> command, final String... aliases) {
    Preconditions.checkArgument(aliases.length > 0, "no aliases provided");
    register(aliases[0], command, Arrays.copyOfRange(aliases, 1, aliases.length));
  }

  @Override
  public void register(final String alias, final Command<?> command, final String... otherAliases) {
    Preconditions.checkNotNull(alias, "alias");
    Preconditions.checkNotNull(otherAliases, "otherAliases");
    Preconditions.checkNotNull(command, "executor");

    // TODO
    //RawCommand rawCmd = RegularCommandWrapper.wrap(command);
    //this.commands.put(alias.toLowerCase(Locale.ENGLISH), rawCmd);

    for (int i = 0, length = otherAliases.length; i < length; i++) {
      final String alias1 = otherAliases[i];
      Preconditions.checkNotNull(alias1, "alias at index %s", i + 1);
      // TODO
      //this.commands.put(alias1.toLowerCase(Locale.ENGLISH), rawCmd);
    }
  }

  @Override
  public void unregister(final String alias) {
    Preconditions.checkNotNull(alias, "name");
    // TODO
    //this.commands.remove(alias.toLowerCase(Locale.ENGLISH));
  }

  /*private void register(final CommandNode<CommandSource> node) {
    Preconditions.checkNotNull(node, "node");
    dispatcher.getRoot().addChild(node);
  }*/

  // General

  public CommandDispatcher<CommandSource> getDispatcher() {
    return dispatcher;
  }

  private <C extends CommandInvocation> Command<C> getCommand(final String alias) {
    //noinspection unchecked
    return (Command<C>) commands.get(alias.toLowerCase(Locale.ENGLISH));
  }

  public boolean hasCommand(final String alias) {
    return commands.containsKey(alias.toLowerCase(Locale.ENGLISH));
  }

  public Set<String> getAllRegisteredCommands() {
    return ImmutableSet.copyOf(commands.keySet());
  }

  private <C extends CommandInvocation> C createContext(
          final Command<C> command, final CommandSource source, final String alias, final String args) {
    CommandInvocationFactory<?> factory = contextFactories.getOrDefault(
            command.getClass(), CommandInvocationFactory.FALLBACK);
    String commandLine = factory.includeAlias() ? args : (alias + " " + args);

    //noinspection unchecked
    return (C) factory.create(source, commandLine);
  }

  // Execution

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
  public CompletableFuture<Boolean> execute(final CommandSource source, final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    return callCommandEvent(source, cmdLine).thenApply(event -> {
      CommandResult commandResult = event.getResult();
      if (commandResult.isForwardToServer() || !commandResult.isAllowed()) {
        return false;
      }

      String command = commandResult.getCommand().orElse(event.getCommand());
      return executeImmediately0(source, command);
    });
  }

  @Override
  public CompletableFuture<Boolean> executeImmediately(final CommandSource source, final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    return CompletableFuture.supplyAsync(
            () -> executeImmediately0(source, cmdLine), eventManager.getService());
  }

  private <C extends CommandInvocation> boolean executeImmediately0(final CommandSource source,
                                                                    final String cmdLine) {
    String alias = cmdLine;
    String args = "";
    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace != -1) {
      alias = cmdLine.substring(0, firstSpace);
      args = cmdLine.substring(firstSpace);
    }

    Command<C> command = getCommand(alias);
    if (command == null) {
      // Alias isn't registered, don't parse it
      return false;
    }

    C context = createContext(command, source, alias, args);
    try {
      if (!command.hasPermission(context)) {
        return false;
      }

      command.execute(context);
      return true;
    } catch (final Exception e) {
      if (e.getCause() instanceof CommandSyntaxException) {
        // TODO Send invalid syntax message to player (exception contains details)
        return false;
      }

      throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
    }
  }

  // Suggestions

  private <C extends CommandInvocation> CompletableFuture<List<String>> offerSuggestions(
          final CommandSource source, final String alias, final String args) {
    Command<C> command = getCommand(alias);
    if (command == null) {
      // No such command, so we can't offer any tab complete suggestions.
      return CompletableFuture.completedFuture(ImmutableList.of());
    }

    C context = createContext(command, source, alias, args);
    try {
      if (!command.hasPermission(context)) {
        return CompletableFuture.completedFuture(ImmutableList.of());
      }

      return command.suggestAsync(context).thenApply(ImmutableList::copyOf);
    } catch (final Exception e) {
      if (e.getCause() instanceof CommandSyntaxException) {
        return CompletableFuture.completedFuture(ImmutableList.of());
      }

      String cmdLine = alias + " " + args;
      throw new RuntimeException("Unable to invoke suggestions for command " + cmdLine + " for " + source, e);
    }
  }

  /**
   * Offer suggestions to fill in the command.
   *
   * @param source the source for the command
   * @param cmdLine the partially completed command
   * @return a {@link CompletableFuture} eventually completed with a {@link List}, possibly empty
   */
  public CompletableFuture<List<String>> offerSuggestions(final CommandSource source, final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace == -1) {
      // Offer to fill in commands.
      ImmutableList.Builder<String> availableCommands = ImmutableList.builder();
      for (Map.Entry<String, Command<?>> entry : commands.entrySet()) {
        if (entry.getKey().regionMatches(true, 0, cmdLine, 0, cmdLine.length())
            && hasPermission(entry.getValue(), source, entry.getKey(), "")) {
          availableCommands.add("/" + entry.getKey());
        }
      }
      return CompletableFuture.completedFuture(availableCommands.build());
    }

    String alias = cmdLine.substring(0, firstSpace);
    String args = cmdLine.substring(firstSpace);
    return offerSuggestions(source, alias, args);
  }

  // Permissions

  private <C extends CommandInvocation> boolean hasPermission(
          final Command<C> command, final CommandSource source, final String alias, final String args) {
    C context = createContext(command, source, alias, args);
    return command.hasPermission(context);
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
    Command<?> command = getCommand(alias);
    if (command == null) {
      return false;
    }

    try {
      return hasPermission(command, source, alias, args);
    } catch (final Exception e) {
      if (e.getCause() instanceof CommandSyntaxException) {
        return false;
      }

      throw new RuntimeException(
          "Unable to invoke suggestions for command " + cmdLine + " for " + source, e);
    }
  }
}
