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
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import com.velocitypowered.proxy.util.BrigadierUtils;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

public class VelocityCommandManager implements CommandManager {

  private final CommandDispatcher<CommandSource> dispatcher;
  private final VelocityEventManager eventManager;

  public VelocityCommandManager(final VelocityEventManager eventManager) {
    this.eventManager = Preconditions.checkNotNull(eventManager);
    this.dispatcher = new CommandDispatcher<>();
  }

  @Override
  public CommandMeta.Builder metaBuilder(final String alias) {
    Preconditions.checkNotNull(alias, "alias");
    return new VelocityCommandMeta.Builder(alias);
  }

  @Override
  public CommandMeta.Builder metaBuilder(final BrigadierCommand command) {
    Preconditions.checkNotNull(command, "command");
    return new VelocityCommandMeta.Builder(command.getNode().getName());
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
    register(metaBuilder(alias).aliases(otherAliases).build(), command);
  }

  @Override
  public void register(final BrigadierCommand command) {
    Preconditions.checkNotNull(command, "command");
    register(metaBuilder(command).build(), command);
  }

  @Override
  public void register(final CommandMeta meta, final Command command) {
    Preconditions.checkNotNull(meta, "meta");
    Preconditions.checkNotNull(command, "command");

    Iterator<String> aliasIterator = meta.getAliases().iterator();
    String alias = aliasIterator.next();

    LiteralCommandNode<CommandSource> node = null;
    if (command instanceof BrigadierCommand) {
      node = ((BrigadierCommand) command).getNode();
    } else if (command instanceof SimpleCommand) {
      node = CommandNodeFactory.SIMPLE.create(alias, (SimpleCommand) command);
    } else if (command instanceof RawCommand) {
      // This ugly hack will be removed in Velocity 2.0. Most if not all plugins
      // have side-effect free #suggest methods. We rely on the newer RawCommand
      // throwing UOE.
      RawCommand asRaw = (RawCommand) command;
      try {
        asRaw.suggest(null, new String[0]);
      } catch (final UnsupportedOperationException e) {
        node = CommandNodeFactory.RAW.create(alias, asRaw);
      } catch (final Exception ignored) {
        // The implementation probably relies on a non-null source
      }
    }
    if (node == null) {
      node = CommandNodeFactory.FALLBACK.create(alias, command);
    }

    if (!(command instanceof BrigadierCommand)) {
      if (!meta.getHints().isEmpty()) {
        // If the user specified a hint, then add the hints to the command node directly.
        for (CommandNode<CommandSource> hint : meta.getHints()) {
          node.addChild(hint);
        }
      }
    }

    dispatcher.getRoot().addChild(node);
    while (aliasIterator.hasNext()) {
      String otherAlias = aliasIterator.next();
      CommandNode<CommandSource> existingNode = dispatcher.getRoot()
          .getChild(alias.toLowerCase(Locale.ENGLISH));
      if (existingNode != null) {
        dispatcher.getRoot().getChildren().remove(existingNode);
      }
      dispatcher.getRoot().addChild(BrigadierUtils.buildRedirect(otherAlias, node));
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
  public boolean execute(final CommandSource source, final String cmdLine) {
    return executeAsync(source, cmdLine).join();
  }

  @Override
  public boolean executeImmediately(final CommandSource source, final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    ParseResults<CommandSource> results = parse(cmdLine, source, true);
    try {
      return dispatcher.execute(results) != BrigadierCommand.FORWARD;
    } catch (final CommandSyntaxException e) {
      boolean isSyntaxError = !e.getType().equals(
              CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand());
      if (isSyntaxError) {
        source.sendMessage(TextComponent.of(e.getMessage(), NamedTextColor.RED));
        // This is, of course, a lie, but the API will need to change...
        return true;
      } else {
        return false;
      }
    } catch (final Throwable e) {
      // Ugly, ugly swallowing of everything Throwable, because plugins are naughty.
      throw new RuntimeException("Unable to invoke command " + cmdLine + " for " + source, e);
    }
  }

  @Override
  public CompletableFuture<Boolean> executeAsync(final CommandSource source, final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    return callCommandEvent(source, cmdLine).thenApply(event -> {
      CommandResult commandResult = event.getResult();
      if (commandResult.isForwardToServer() || !commandResult.isAllowed()) {
        return false;
      }
      return executeImmediately(source, commandResult.getCommand().orElse(event.getCommand()));
    });
  }

  @Override
  public CompletableFuture<Boolean> executeImmediatelyAsync(
          final CommandSource source, final String cmdLine) {
    Preconditions.checkNotNull(source, "source");
    Preconditions.checkNotNull(cmdLine, "cmdLine");

    return CompletableFuture.supplyAsync(
        () -> executeImmediately(source, cmdLine), eventManager.getService());
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
    Preconditions.checkNotNull(alias, "alias");
    return dispatcher.getRoot().getChild(alias.toLowerCase(Locale.ENGLISH)) != null;
  }

  public CommandDispatcher<CommandSource> getDispatcher() {
    return dispatcher;
  }
}
