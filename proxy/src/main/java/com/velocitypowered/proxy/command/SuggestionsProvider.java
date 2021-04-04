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
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.spotify.futures.CompletableFutures;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.proxy.ProxyOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

  private static final Logger LOGGER = LogManager.getLogger(ProxyOptions.class);
  private static final StringRange ALIAS_SUGGESTIONS_RANGE = StringRange.at(0);

  private final CommandDispatcher<S> dispatcher;

  public SuggestionsProvider(final CommandDispatcher<S> dispatcher) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
  }

  public CompletableFuture<Suggestions> provideSuggestions(final String input, final S source) {
    final StringReader reader = new StringReader(input);
    final CommandContextBuilder<S> context =
            new CommandContextBuilder<>(dispatcher, source, getRoot(), 0);
    final StringRange aliasRange = this.skipAlias(reader);
    final String alias = aliasRange.get(input);
    final CommandNode<S> aliasNode = getRoot().getChild(alias);

    final boolean hasArguments = reader.canRead();
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
    reader.skip(); // separator
    return this.provideArgumentsSuggestions(aliasNode, reader, context);
  }

  private StringRange skipAlias(final StringReader reader) {
    while (reader.canRead() && reader.peek() != CommandDispatcher.ARGUMENT_SEPARATOR_CHAR) {
      reader.skip();
    }
    return StringRange.between(0, reader.getCursor());
  }

  /**
   * Returns alias suggestions for the given partial input.
   *
   * @param reader the input reader
   * @param context the context containing no nodes
   * @return
   */
  private CompletableFuture<Suggestions> provideAliasSuggestions(
          final StringReader reader, final CommandContextBuilder<S> context) {
    final String input = reader.getString();
    final CommandContext<S> built = context.build(input);
    final Collection<CommandNode<S>> aliases = getRoot().getChildren();

    @SuppressWarnings("unchecked")
    final CompletableFuture<Suggestions>[] futures = new CompletableFuture[aliases.size()];
    int i = 0;
    for (final CommandNode<S> node : aliases) {
      CompletableFuture<Suggestions> future = Suggestions.empty();
      if (node.canUse(context.getSource())) {
        final CommandContextBuilder<S> reqContext = context.copy()
                .withNode(node, ALIAS_SUGGESTIONS_RANGE);
        if (node.canUse(reqContext, reader)) {
          try {
            future = node.listSuggestions(built, new SuggestionsBuilder(input, 0));
          } catch (final CommandSyntaxException e) {
            throw new AssertionError("Alias node cannot list suggestions", e);
          }
        }
      }
      futures[i++] = future;
    }
    return this.merge(input, futures);
  }

  /**
   * Returns the suggestions as given by Brigadier for the given input and source.
   *
   * @param reader the input reader
   * @param source the source to provide suggestions for
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideDefaultSuggestions(final StringReader reader,
                                                                   final S source) {
    final ParseResults<S> parse = dispatcher.parse(reader, source);
    return dispatcher.getCompletionSuggestions(parse);
  }

  /**
   * Merges the suggestions provided by the {@link Command} and the hints given during registration
   * for the given input.
   *
   * @param aliasNode the alias node, possibly a redirect origin
   * @param reader the input reader
   * @param contextSoFar the context containing {@code aliasNode}
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideArgumentsSuggestions(
          final CommandNode<S> aliasNode, final StringReader reader,
          final CommandContextBuilder<S> contextSoFar) {
    final S source = contextSoFar.getSource();
    final CommandNode<S> targetAliasNode = aliasNode.getRedirect() == null
            ? aliasNode
            : aliasNode.getRedirect();
    final CommandNode<S> argumentsNode = targetAliasNode.getChild(VelocityCommands.ARGS_NODE_NAME);
    if (!VelocityCommands.isArgumentsNode(argumentsNode)) {
      // This is a BrigadierCommand, fallback to regular suggestions
      reader.setCursor(0);
      return this.provideDefaultSuggestions(reader, source);
    }
    if (!argumentsNode.canUse(source)) {
      return Suggestions.empty();
    }

    final int start = reader.getCursor();
    final CommandContextBuilder<S> context = contextSoFar.copy();
    try {
      argumentsNode.parse(reader, context);
      if (reader.canRead()) {
        // The ArgumentType of the arguments node is greedy
        throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherParseException()
                .createWithContext(reader, context);
      }
    } catch (final CommandSyntaxException e) {
      // The ArgumentType can parse any string
      throw new AssertionError("Arguments node cannot parse arguments", e);
    }
    if (!argumentsNode.canUse(context, reader)) {
      return Suggestions.empty();
    }
    // Ask the command for suggestions via the arguments node
    reader.setCursor(start);
    final CompletableFuture<Suggestions> commandSuggestions =
            this.getArgumentsNodeSuggestions(argumentsNode, reader, context);
    if (targetAliasNode.getChildren().size() == 1) {
      return commandSuggestions; // command has no hints
    }

    // Parse the hints (ignoring the arguments node) to get remaining suggestions
    reader.setCursor(start);
    final CompletableFuture<Suggestions> hintSuggestions =
            this.getHintSuggestions(aliasNode, targetAliasNode, reader, contextSoFar);

    return this.merge(reader.getString(), commandSuggestions, hintSuggestions);
  }

  /**
   * Returns the suggestions provided by the {@link Command} for the given input.
   *
   * @param argumentsNode the arguments node of the command
   * @param reader the input reader
   * @param context the context containing an alias node and {@code argumentsNode}
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> getArgumentsNodeSuggestions(
          final CommandNode<S> argumentsNode, final StringReader reader,
          final CommandContextBuilder<S> context) {
    final int start = reader.getCursor();
    final String fullInput = reader.getString();
    final String truncatedInput = reader.getRead();
    final CommandContext<S> built = context.build(truncatedInput);
    try {
      return argumentsNode.listSuggestions(built, new SuggestionsBuilder(fullInput, start));
    } catch (final CommandSyntaxException e) {
      LOGGER.error("Arguments node cannot provide suggestions, skipping", e);
      return Suggestions.empty();
    }
  }

  /**
   * Returns the suggestions provided by the matched hinting nodes for the given input.
   *
   * @param aliasNode the alias node, possibly a redirect origin
   * @param targetAliasNode the primary alias node, possibly the target of a redirect
   * @param originalReader the input reader
   * @param originalContext the context containing {@code aliasNode}
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> getHintSuggestions(
          final CommandNode<S> aliasNode, final CommandNode<S> targetAliasNode,
          final StringReader originalReader, final CommandContextBuilder<S> originalContext) {
    CommandContextBuilder<S> context = originalContext;
    StringReader reader = originalReader;
    if (aliasNode != targetAliasNode) {
      context = new CommandContextBuilder<>(dispatcher, originalContext.getSource(), getRoot(), 0);
      final String fabricatedInput = targetAliasNode.getName()
              + CommandDispatcher.ARGUMENT_SEPARATOR_CHAR
              + originalReader.getRemaining();
      reader = new StringReader(fabricatedInput);
      try {
        targetAliasNode.parse(reader, context);
      } catch (final CommandSyntaxException e) {
        throw new AssertionError("Alias node cannot parse fabricated input", e);
      }
      reader.skip(); // argument separator
    }

    final ParseResults<S> parse = this.parseHints(targetAliasNode, reader, context);
    return dispatcher.getCompletionSuggestions(parse);
  }

  /**
   * Parses the hinting nodes under the given node, which is either the primary alias node of
   * a {@link Command} or another hinting node.
   *
   * @param node the node to parse
   * @param originalReader the input reader
   * @param contextSoFar the context containing the primary alias node of the command
   * @return the parse results, including the parsed hinting nodes
   */
  private ParseResults<S> parseHints(final CommandNode<S> node,
                                     final StringReader originalReader,
                                     final CommandContextBuilder<S> contextSoFar) {
    // This is a stripped-down version of CommandDispatcher#parseNodes that doesn't perform
    // requirement checks and doesn't handle redirects, which are not used by hints.
    for (final CommandNode<S> child : node.getRelevantNodes(originalReader)) {
      if (VelocityCommands.isArgumentsNode(child)) {
        continue;
      }
      final CommandContextBuilder<S> context = contextSoFar.copy();
      final StringReader reader = new StringReader(originalReader);
      try {
        child.parse(reader, context);
        if (reader.canRead() && reader.peek() != CommandDispatcher.ARGUMENT_SEPARATOR_CHAR) {
          continue;
        }
      } catch (final CommandSyntaxException ignored) {
        continue;
      }
      if (reader.canRead(2)) {
        reader.skip();
        return this.parseHints(child, reader, context);
      } else {
        return new ParseResults<>(context, reader, Collections.emptyMap());
      }
    }
    return new ParseResults<>(contextSoFar, originalReader, Collections.emptyMap());
  }

  /**
   * Returns a future that is completed with the result of merging the given suggestions when
   * all of the futures complete. The results of the futures that complete exceptionally are
   * ignored.
   *
   * @param fullInput the command input
   * @param futures the futures that complete with the suggestion results
   * @return the future that completes with the merged suggestions
   */
  @SafeVarargs
  private final CompletableFuture<Suggestions> merge(
          final String fullInput, final CompletableFuture<Suggestions>... futures) {
    // https://github.com/Mojang/brigadier/pull/81
    return CompletableFuture.allOf(futures).handle((unused, throwable) -> {
      final List<Suggestions> suggestions = new ArrayList<>(futures.length);
      for (final CompletableFuture<Suggestions> future : futures) {
        if (!future.isCompletedExceptionally()) {
          suggestions.add(future.join());
        }
      }
      return Suggestions.merge(fullInput, suggestions);
    });
  }

  private RootCommandNode<S> getRoot() {
    return dispatcher.getRoot();
  }
}
