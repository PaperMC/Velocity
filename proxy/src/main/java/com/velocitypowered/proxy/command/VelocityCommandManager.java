/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.command.CommandExecuteEvent.CommandResult;
import com.velocitypowered.proxy.command.registrar.BrigadierCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.CommandRegistrar;
import com.velocitypowered.proxy.command.registrar.LegacyCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.RawCommandRegistrar;
import com.velocitypowered.proxy.command.registrar.SimpleCommandRegistrar;
import com.velocitypowered.proxy.plugin.VelocityEventManager;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class VelocityCommandManager implements CommandManager {

  private final CommandDispatcher<CommandSource> dispatcher;
  private final VelocityEventManager eventManager;

  private final SuggestionProvider<CommandSource> suggestionsProvider;
  private final CommandTreeInjector<CommandSource> injector;
  private final List<CommandRegistrar<?>> registrars;

  /**
   * Constructs a command manager.
   *
   * @param eventManager the event manager
   */
  public VelocityCommandManager(final VelocityEventManager eventManager) {
    this.eventManager = Preconditions.checkNotNull(eventManager);
    this.dispatcher = new CommandDispatcher<>();
    this.suggestionsProvider = new SuggestionProvider<>(this.dispatcher);
    this.injector = new CommandTreeInjector<>(this.dispatcher);
    this.registrars = ImmutableList.of(
            new BrigadierCommandRegistrar(this.dispatcher),
            new SimpleCommandRegistrar(this.dispatcher),
            new RawCommandRegistrar(this.dispatcher),
            new LegacyCommandRegistrar(this.dispatcher));
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

    for (final CommandRegistrar<?> registrar : this.registrars) {
      if (this.tryRegister(registrar, command, meta)) {
        return;
      }
    }
    // TODO(velocity-2): throw IAE here (command doesn't implement any supported superinterface)
    // For now the legacy registrar is a catch-all and we shouldn't reach this.
    throw new AssertionError("Got unregistrable command " + command);
  }

  private <T extends Command> boolean tryRegister(final CommandRegistrar<T> registrar,
                                                  final Command command, final CommandMeta meta) {
    final Class<T> superClass = registrar.registrableSuperInterface();
    if (!superClass.isInstance(command)) {
      return false;
    }
    try {
      registrar.register(superClass.cast(command), meta);
      return true;
    } catch (final IllegalArgumentException ignored) {
      return false;
    }
  }

  @Override
  public void unregister(final String alias) {
    Preconditions.checkNotNull(alias, "alias");
    // The literals of secondary aliases will preserve the children of
    // the removed literal in the graph.
    dispatcher.getRoot().removeChildByName(alias.toLowerCase(Locale.ENGLISH));
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

    final String normalizedInput = this.normalizeInput(cmdLine, true);
    final ParseResults<CommandSource> parse = dispatcher.parse(normalizedInput, source);
    try {
      return dispatcher.execute(parse) != BrigadierCommand.FORWARD;
    } catch (final CommandSyntaxException e) {
      boolean isSyntaxError = !e.getType().equals(
              CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownCommand());
      if (isSyntaxError) {
        source.sendMessage(Identity.nil(), Component.text(e.getMessage(), NamedTextColor.RED));
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
      final CommandResult commandResult = event.getResult();
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

    final String normalizedInput = this.normalizeInput(cmdLine, false);
    try {
      return suggestionsProvider.provideSuggestions(normalizedInput, source)
              .thenApply(suggestions ->
                    Lists.transform(suggestions.getList(), Suggestion::getText));
    } catch (final Exception e) {
      return CompletableFutures.exceptionallyCompletedFuture(e);
    }
  }

  /**
   * Normalizes the given command input.
   *
   * @param cmdLine the raw command input, without the leading slash ('/')
   * @param trim whether to remove leading and trailing whitespace from the input
   * @return the normalized command input
   */
  private String normalizeInput(final String cmdLine, final boolean trim) {
    final String command = trim ? cmdLine.trim() : cmdLine;
    int firstSep = command.indexOf(CommandDispatcher.ARGUMENT_SEPARATOR_CHAR);
    if (firstSep != -1) {
      // Aliases are case-insensitive, arguments are not
      return command.substring(0, firstSep).toLowerCase(Locale.ENGLISH)
              + command.substring(firstSep);
    } else {
      return command.toLowerCase(Locale.ENGLISH);
    }
  }

  /**
   * Returns whether the given alias is registered on this manager.
   *
   * @param alias the command alias to check
   * @return {@code true} if the alias is registered
   */
  @Override
  public boolean hasCommand(final String alias) {
    Preconditions.checkNotNull(alias, "alias");
    return dispatcher.getRoot().getChild(alias.toLowerCase(Locale.ENGLISH)) != null;
  }

  public CommandTreeInjector<CommandSource> getInjector() {
    return injector;
  }
}
