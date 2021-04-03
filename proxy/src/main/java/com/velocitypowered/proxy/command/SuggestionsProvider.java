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
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Provides suggestions for a given command input.
 *
 * <p>Similar to {@link CommandDispatcher#getCompletionSuggestions(ParseResults)}, except it
 * does not require to fully parse the given input and performs exactly 1 requirement predicate
 * check per considered node.
 *
 * @param <S> the type of the command source
 */
final class SuggestionsProvider<S> {

  private final CommandDispatcher<S> dispatcher;

  public SuggestionsProvider(final CommandDispatcher<S> dispatcher) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
  }

  public CompletableFuture<Suggestions> provideSuggestions(final String command, final S source) {
    final StringReader reader = new StringReader(command);
    final CommandContextBuilder<S> context =
            new CommandContextBuilder<>(dispatcher, source, getRoot(), 0);
    while (reader.canRead() && reader.peek() != ' ') {
      reader.skip();
    }
    final StringRange aliasRange = StringRange.between(0, reader.getCursor());
    final String alias = aliasRange.get(reader);
    final boolean hasArguments = reader.canRead();

    final CommandNode<S> aliasNode = getRoot().getChild(alias);
    if (aliasNode == null) {
      if (hasArguments) {
        // Input has arguments for non-registered alias
        return Suggestions.empty();
      }
      return this.provideAliasSuggestions(reader, context);
    } else if (!hasArguments) {
      // Input is a non-registered alias
      return this.provideAliasSuggestions(reader, context);
    }
    context.withNode(aliasNode, aliasRange);
    reader.skip();
    return this.provideArgumentsSuggestions(reader, context, aliasNode);
  }

  private CompletableFuture<Suggestions> provideAliasSuggestions(
          final StringReader reader, final CommandContextBuilder<S> context) {
    final String input = reader.getRead();
    final Collection<CommandNode<S>> aliases = getRoot().getChildren();

    @SuppressWarnings("unchecked")
    final CompletableFuture<Suggestions>[] futures = new CompletableFuture[aliases.size()];
    int i = 0;
    for (final CommandNode<S> node : aliases) {
      CompletableFuture<Suggestions> future = Suggestions.empty();
      if (node.canUse(context.getSource())) {
        final StringRange range = StringRange.between(0, node.getName().length());
        final CommandContextBuilder<S> reqContext = context.copy().withNode(node, range);
        if (node.canUse(reqContext, reader)) {
          final CommandContext<S> builtContext = context.build(input);
          try {
            future = node.listSuggestions(builtContext, new SuggestionsBuilder(input, 0));
          } catch (final CommandSyntaxException e) {
            throw new AssertionError("Cannot list suggestions of alias node", e);
          }
        }
      }
      futures[i++] = future;
    }
    return this.merge(input, futures);
  }

  private CompletableFuture<Suggestions> provideArgumentsSuggestions(
          final StringReader reader, final CommandContextBuilder<S> context,
          final CommandNode<S> aliasNode) {
    final CommandNode<S> argumentsNode = aliasNode.getChild(VelocityCommands.ARGS_NODE_NAME);
    if (!VelocityCommands.isArgumentsNode(argumentsNode)) {
      // This is a BrigadierCommand
      reader.setCursor(0);
      return this.provideDefaultSuggestions(reader, context.getSource());
    }

    final int start = reader.getCursor();
    final String truncatedInput = reader.getRead(); // <alias> + ' '
    try {
      argumentsNode.parse(reader, context);
      if (reader.canRead()) {
        // The ArgumentType of the arguments node is greedy
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().create();
      }
    } catch (final CommandSyntaxException e) {
      // The ArgumentType is able to parse any string
      throw new AssertionError("Arguments node could not parse arguments", e);
    }

    if (!argumentsNode.canUse(context.getSource()) || !argumentsNode.canUse(context, reader)) {
      return Suggestions.empty();
    }

    final CommandContext<S> builtContext = context.build(truncatedInput);
    final Collection<CommandNode<S>> children = aliasNode.getChildren(); // hints + arguments node

    @SuppressWarnings("unchecked")
    final CompletableFuture<Suggestions>[] futures = new CompletableFuture[children.size()];
    int i = 0;
    for (final CommandNode<S> node : children) {
      CompletableFuture<Suggestions> future = new CompletableFuture<>();
      try {
        future = node.listSuggestions(
                builtContext, new SuggestionsBuilder(reader.getString(), start));
      } catch (final CommandSyntaxException ignored) {
        // skip this node's suggestions
      }
      futures[i++] = future;
    }
    return this.merge(reader.getString(), futures);
  }

  private CompletableFuture<Suggestions> provideDefaultSuggestions(final StringReader reader,
                                                                   final S source) {
    final ParseResults<S> parse = dispatcher.parse(reader, source);
    return dispatcher.getCompletionSuggestions(parse);
  }

  /**
   * Returns a future that is completed with the result of merging the given suggestions when
   * all of the futures complete. If any future completes exceptionally, then the returned
   * future also does so.
   *
   * @param fullInput the command input
   * @param futures the futures that complete with the suggestion results
   * @return the future that completes with the merged suggestions
   */
  private CompletableFuture<Suggestions> merge(final String fullInput,
                                               final CompletableFuture<Suggestions>[] futures) {
    return CompletableFuture.allOf(futures).thenApply(unused -> {
      final List<Suggestions> suggestions = new ArrayList<>(futures.length);
      for (final CompletableFuture<Suggestions> future : futures) {
        suggestions.add(future.join());
      }
      return Suggestions.merge(fullInput, suggestions);
    });
  }

  private RootCommandNode<S> getRoot() {
    return dispatcher.getRoot();
  }
}
