package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.*;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class VelocityCommandManager implements CommandManager {

  public static final int NO_PERMISSION = 0xF6287429;
  public static final String ARGUMENTS_NAME = "arguments";

  static LiteralCommandNode<CommandSource> createRawArgsNode(
          final String alias, final com.mojang.brigadier.Command<CommandSource> brigadierCommand,
          SuggestionProvider<CommandSource> suggestionProvider) {
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .executes(brigadierCommand)
            .build();
    CommandNode<CommandSource> arguments = RequiredArgumentBuilder
            .<CommandSource, String>argument(ARGUMENTS_NAME, StringArgumentType.greedyString())
            .suggests(suggestionProvider)
            .executes(brigadierCommand)
            .build();
    node.addChild(arguments);
    return node;
  }

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

  private void registerRedirect(final CommandNode<CommandSource> destination, final String alias) {
    LiteralArgumentBuilder<CommandSource> builder =
            LiteralArgumentBuilder.literal(alias.toLowerCase(Locale.ENGLISH));

    if (!destination.getChildren().isEmpty()) {
      builder = builder.redirect(destination);
    } else {
      // Redirects don't work for nodes without children (argument-less commands).
      // See https://github.com/Mojang/brigadier/issues/46).
      // Manually construct redirect instead (LiteralCommandNode.createBuilder)
      // TODO I suspect the #forward call isn't needed
      builder.requires(destination.getRequirement());
      builder.forward(
              destination.getRedirect(), destination.getRedirectModifier(), destination.isFork());
      builder.executes(builder.getCommand());
    }
    dispatcher.register(builder);
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
      registerRedirect(node, alias1);
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
    try {
      return dispatcher.execute(cmdLine, source) != NO_PERMISSION;
    } catch (final CommandSyntaxException e) {
      return false;
    } catch (final Exception e) {
      throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
    }
  }

  public boolean hasCommand(final String alias) {
    Preconditions.checkNotNull(alias);
    return dispatcher.getRoot().getChild(alias.toLowerCase(Locale.ENGLISH)) != null;
  }

  public CompletableFuture<List<String>> offerSuggestions(final CommandSource source,
                                                          final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    ParseResults<CommandSource> parse = dispatcher.parse(cmdLine, source);
    return dispatcher.getCompletionSuggestions(parse)
            .thenApply(suggestions -> Lists.transform(suggestions.getList(), Suggestion::getText));
  }
}
