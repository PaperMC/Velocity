package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import com.velocitypowered.proxy.util.BrigadierUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class VelocityCommandManager implements CommandManager {

  private final CommandDispatcher<CommandSource> dispatcher;
  private final VelocityEventManager eventManager;

  public VelocityCommandManager(final VelocityEventManager eventManager) {
    this.eventManager = Preconditions.checkNotNull(eventManager);
    this.dispatcher = new CommandDispatcher<>();
  }

  @Override
  public BrigadierCommand.Builder brigadierBuilder() {
    return new VelocityBrigadierCommand.Builder(this);
  }

  @Override
  public void register(final Command command, final String... aliases) {
    Preconditions.checkArgument(aliases.length > 0, "no aliases provided");
    register(aliases[0], command, Arrays.copyOfRange(aliases, 1, aliases.length));
  }

  @Override
  public void register(final String alias, final Command command, final String... otherAliases) {
    Preconditions.checkNotNull(alias, "alias");
    Preconditions.checkNotNull(command, "command");
    Preconditions.checkNotNull(otherAliases, "otherAliases");
    Preconditions.checkArgument(!hasCommand(alias), "alias already registered");

    LiteralCommandNode<CommandSource> node;
    if (command instanceof VelocityBrigadierCommand) {
      node = ((VelocityBrigadierCommand) command).getNode();
    } else if (command instanceof LegacyCommand) {
      node = CommandNodeFactory.LEGACY.create(alias, (LegacyCommand) command);
    } else if (command instanceof RawCommand) {
      node = CommandNodeFactory.RAW.create(alias, (RawCommand) command);
    } else {
      node = CommandNodeFactory.FALLBACK.create(alias, command);
    }
    dispatcher.getRoot().addChild(node);

    for (int i = 0, length = otherAliases.length; i < length; i++) {
      final String alias1 = otherAliases[i];
      Preconditions.checkNotNull(alias1, "alias at index %s", i + 1);
      Preconditions.checkArgument(!hasCommand(alias1),
              "alias at index %s already registered", i + 1);
      dispatcher.getRoot().addChild(BrigadierUtils.buildRedirect(alias1, node));
    }
  }

  @Override
  public void unregister(final String alias) {
    Preconditions.checkNotNull(alias, "alias");
    CommandNode<CommandSource> node =
            dispatcher.getRoot().getChild(alias.toLowerCase(Locale.ENGLISH));

    if (node != null) {
      dispatcher.getRoot().getChildren().remove(node);
    }
  }

  /**
   * Fires a {@link CommandExecuteEvent}.
   *
   * @param source the source to execute the command for
   * @param cmdLine the command to execute
   * @return the {@link CompletableFuture} of the event
   */
  public CompletableFuture<CommandExecuteEvent> callCommandEvent(final CommandSource source,
                                                                 final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");
    return eventManager.fire(new CommandExecuteEvent(source, cmdLine));
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

  private boolean executeImmediately0(final CommandSource source, final String cmdLine) {
    ParseResults<CommandSource> parse = parse(cmdLine, source, true);
    try {
      return dispatcher.execute(parse) != BrigadierUtils.NO_PERMISSION;
    } catch (final CommandSyntaxException e) {
      // TODO Distinguish between syntax errors and missing command
      return false;
    } catch (final Exception e) {
      throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
    }
  }

  /**
   * Returns suggestions to fill in the given command.
   *
   * @param source the source to execute the command for
   * @param cmdLine the partially completed command
   * @return a {@link CompletableFuture} eventually completed with a {@link List},
   *         possibly empty
   */
  public CompletableFuture<List<String>> offerSuggestions(final CommandSource source,
                                                          final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    ParseResults<CommandSource> parse = parse(cmdLine, source, false);
    return dispatcher.getCompletionSuggestions(parse)
            .thenApply(suggestions -> Lists.transform(suggestions.getList(), Suggestion::getText));
  }

  private ParseResults<CommandSource> parse(final String cmdLine, final CommandSource source,
                                            final boolean trim) {
    String normalized = BrigadierUtils.normalizeInput(cmdLine, trim);
    return dispatcher.parse(normalized, source);
  }

  /**
   * Returns whether the given alias is registered on this manager.
   *
   * @param alias the command alias to check
   * @return {@code true} if the alias is registered
   */
  public boolean hasCommand(final String alias) {
    Preconditions.checkNotNull(alias);
    return dispatcher.getRoot().getChild(alias.toLowerCase(Locale.ENGLISH)) != null;
  }

  public CommandDispatcher<CommandSource> getDispatcher() {
    return dispatcher;
  }
}
