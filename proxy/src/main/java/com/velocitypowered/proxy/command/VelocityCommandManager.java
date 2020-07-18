package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class VelocityCommandManager implements CommandManager {

  // `commands` contains all registered command case-insensitive aliases.
  // Multiple aliases may map to the same command.
  //
  // On execution, suggestion offers and permission checks, the corresponding Command object
  // is retrieved. Then, a CommandInvocation describing the request is created by
  // the invocation factory registry. This object is then passed to the underlying command,
  // which may use the Brigadier dispatcher iff it implements BrigadierCommand.
  // For legacy reasons, this manager should avoid calling command methods directly.
  // Instead, use the corresponding methods that take a command and invocation
  // as arguments on this class.
  //
  // By design, the API doesn't provide CommandInvocation implementations.
  // Commands are not meant to be executed directly. Instead, users should
  // call CommandManager#execute.

  private final Map<String, Command<?>> commands = new HashMap<>();

  private final CommandInvocationFactoryRegistry invocationFactory =
          new CommandInvocationFactoryRegistry();
  private final CommandDispatcher<CommandSource> brigadierDispatcher = new CommandDispatcher<>();

  private final VelocityEventManager eventManager;

  public VelocityCommandManager(final VelocityEventManager eventManager) {
    this.eventManager = eventManager;
  }

  @Override
  public BrigadierCommand.Builder brigadierBuilder() {
    return new VelocityBrigadierCommand.Builder(this);
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

    this.commands.put(alias.toLowerCase(Locale.ENGLISH), command);

    for (int i = 0, length = otherAliases.length; i < length; i++) {
      final String alias1 = otherAliases[i];
      Preconditions.checkNotNull(alias1, "alias at index %s", i + 1);
      this.commands.put(alias1.toLowerCase(Locale.ENGLISH), command);
    }
  }

  @Override
  public void unregister(final String alias) {
    Preconditions.checkNotNull(alias, "name");
    Command<?> removed = this.commands.remove(alias.toLowerCase(Locale.ENGLISH));

    if (removed instanceof VelocityBrigadierCommand) {
      VelocityBrigadierCommand asBrigadier = (VelocityBrigadierCommand) removed;

      // getChildren() returns the values() view of the internal node map.
      // Brigadier doesn't hardcode the redirects, so we don't care if
      // the node to remove is the registered node or a redirect.
      // Either way, all other registered aliases will still work (see javadoc).
      brigadierDispatcher.getRoot().getChildren().remove(asBrigadier.getNode());
    }
  }

  // General

  public CommandDispatcher<CommandSource> getBrigadierDispatcher() {
    return brigadierDispatcher;
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

  // Execution

  /**
   * Fires a {@link CommandExecuteEvent}.
   *
   * @param source the command's source
   * @param cmd the command
   * @return the posted event
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
  public CompletableFuture<Boolean> executeImmediately(final CommandSource source,
                                                       final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    return CompletableFuture.supplyAsync(
        () -> executeImmediately0(source, cmdLine), eventManager.getService());
  }

  private <I extends CommandInvocation> void execute(
          final Command<I> command, final I invocation) {
    if (invocation instanceof VelocityLegacyCommandInvocation) {
      VelocityLegacyCommandInvocation legacy = ((VelocityLegacyCommandInvocation) invocation);

      if (legacy.shouldCallDeprecatedMethods()) {
        command.execute(invocation.source(), legacy.arguments());
        return;
      }
    }
    command.execute(invocation);
  }

  private <I extends CommandInvocation> boolean executeImmediately0(final CommandSource source,
                                                                    final String cmdLine) {
    String alias = cmdLine;
    String args = "";
    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace != -1) {
      alias = cmdLine.substring(0, firstSpace);
      args = cmdLine.substring(firstSpace);
    }

    Command<I> command = getCommand(alias);
    if (command == null) {
      return false;
    }

    I invocation = invocationFactory.createInvocation(command, source, alias, args);
    try {
      if (!hasPermission(command, invocation)) {
        return false;
      }

      execute(command, invocation);
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

  private <I extends CommandInvocation> CompletableFuture<List<String>> suggestAsync(
          final Command<I> command, final I invocation) {
    if (invocation instanceof VelocityLegacyCommandInvocation) {
      VelocityLegacyCommandInvocation legacy = ((VelocityLegacyCommandInvocation) invocation);

      if (legacy.shouldCallDeprecatedMethods()) {
        return command.suggestAsync(legacy.source(), legacy.arguments());
      }
    }

    return command.suggestAsync(invocation);
  }

  private <I extends CommandInvocation> CompletableFuture<List<String>> offerSuggestions(
          final CommandSource source, final String alias, final String args) {
    Command<I> command = getCommand(alias);
    if (command == null) {
      // No such command, so we can't offer any tab complete suggestions.
      return CompletableFuture.completedFuture(ImmutableList.of());
    }

    I invocation = invocationFactory.createInvocation(command, source, alias, args);
    try {
      if (!hasPermission(command, invocation)) {
        return CompletableFuture.completedFuture(ImmutableList.of());
      }

      return suggestAsync(command, invocation).thenApply(ImmutableList::copyOf);
    } catch (final Exception e) {
      if (e.getCause() instanceof CommandSyntaxException) {
        return CompletableFuture.completedFuture(ImmutableList.of());
      }

      String cmdLine = alias + " " + args;
      throw new RuntimeException(
              "Unable to invoke suggestions for command " + cmdLine + " for " + source, e);
    }
  }

  /**
   * Offer suggestions to fill in the command.
   *
   * @param source the source for the command
   * @param cmdLine the partially completed command
   * @return a {@link CompletableFuture} eventually completed with a {@link List}, possibly empty
   */
  public CompletableFuture<List<String>> offerSuggestions(final CommandSource source,
                                                          final String cmdLine) {
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

  private <I extends CommandInvocation> boolean hasPermission(
          final Command<I> command, final I invocation) {
    if (invocation instanceof VelocityLegacyCommandInvocation) {
      VelocityLegacyCommandInvocation legacy = ((VelocityLegacyCommandInvocation) invocation);

      if (legacy.shouldCallDeprecatedMethods()) {
        return command.hasPermission(invocation.source(), legacy.arguments());
      }
    }

    return command.hasPermission(invocation);
  }

  private <I extends CommandInvocation> boolean hasPermission(
          final Command<I> command, final CommandSource source,
          final String alias, final String args) {
    I invocation = invocationFactory.createInvocation(command, source, alias, args);
    return hasPermission(command, invocation);
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

  private final class CommandInvocationFactoryRegistry {

    private final Map<Class<? extends Command<?>>, CommandInvocationFactory<?>> factories;

    private CommandInvocationFactoryRegistry() {
      // We might allow external invocation factory registrations in the future.
      this.factories = ImmutableMap
              .<Class<? extends Command<?>>, CommandInvocationFactory<?>>builder()
              .put(LegacyCommand.class, VelocityLegacyCommandInvocation.FACTORY)
              .put(BrigadierCommand.class,
                      new VelocityBrigadierCommandInvocation.Factory(brigadierDispatcher))
              .put(RawCommand.class, VelocityRawCommandInvocation.FACTORY)
              .build();
    }

    private <I extends CommandInvocation> CommandInvocationFactory<I> getFactory(
            final Command<I> command) {
      for (Map.Entry<Class<? extends Command<?>>, CommandInvocationFactory<?>> entry
              : this.factories.entrySet()) {
        if (entry.getKey().isInstance(command)) {
          //noinspection unchecked
          return (CommandInvocationFactory<I>) entry.getValue();
        }
      }

      try {
        //noinspection unchecked
        return (CommandInvocationFactory<I>) CommandInvocationFactory.FALLBACK;
      } catch (final ClassCastException e) {
        throw new RuntimeException("Custom Command interfaces are not supported at this time", e);
      }
    }

    public <I extends CommandInvocation> I createInvocation(
            final Command<I> command, final CommandSource source,
            final String alias, final String args) {
      CommandInvocationFactory<I> factory = getFactory(command);
      String commandLine = factory.includeAlias() ? (alias + " " + args) : args;
      return factory.create(source, commandLine);
    }
  }
}
