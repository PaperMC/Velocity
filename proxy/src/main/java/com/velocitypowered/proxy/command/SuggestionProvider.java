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
import com.velocitypowered.api.command.Command;
import com.velocitypowered.proxy.command.brigadier.GreedyArgumentCommandNode;
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
 * <p>Similar to {@link CommandDispatcher#getCompletionSuggestions(ParseResults)}, except
 * it does not fully parse the given input and performs exactly 1 requirement predicate
 * check per considered node.
 *
 * @param <S> the type of the command source
 */
final class SuggestionProvider<S> {

  private static final Logger LOGGER = LogManager.getLogger(SuggestionProvider.class);

  private static final StringRange ALIAS_SUGGESTION_RANGE = StringRange.at(0);

  private final CommandDispatcher<S> dispatcher;

  SuggestionProvider(final CommandDispatcher<S> dispatcher) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
  }

  /**
   * Provides suggestions for the given input and source.
   *
   * @param input the partial input
   * @param source the command source
   * @return a future that completes with the suggestions
   */
  public CompletableFuture<Suggestions> provideSuggestions(final String input, final S source) {
    final CommandContextBuilder<S> context = new CommandContextBuilder<>(
            this.dispatcher, source, this.root(), 0);
    return this.provideSuggestions(new StringReader(input), context);
  }

  /**
   * Provides suggestions for the given input and context.
   *
   * @param reader the input reader
   * @param context an empty context object
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideSuggestions(final StringReader reader,
                                                           final CommandContextBuilder<S> context) {
    final StringRange aliasRange = this.skipAlias(reader);
    final String alias = aliasRange.get(reader); // TODO #toLowerCase and test
    final LiteralCommandNode<S> literal = (LiteralCommandNode<S>) this.root().getChild(alias);

    final boolean hasArguments = reader.canRead();
    if (hasArguments) {
      if (literal == null) {
        // Input has arguments for non-registered alias
        return Suggestions.empty();
      }
      context.withNode(literal, aliasRange);
      reader.skip(); // separator
      return this.provideArgumentsSuggestions(literal, reader, context);
    } else {
      return this.provideAliasSuggestions(reader, context);
    }
  }

  private StringRange skipAlias(final StringReader reader) {
    final int firstSep = reader.getString()
            .indexOf(CommandDispatcher.ARGUMENT_SEPARATOR_CHAR, reader.getCursor());
    final StringRange range = StringRange
            .between(reader.getCursor(), firstSep == -1 ? reader.getTotalLength() : firstSep);
    reader.setCursor(range.getEnd());
    return range;
  }

  /**
   * Returns whether a literal node with the given name should be considered for
   * suggestions given the specified input.
   *
   * @param literal the literal
   * @param input the partial input
   * @return true if the literal should be considered; false otherwise
   */
  private static boolean shouldConsider(final String literal, final String input) {
    return literal.regionMatches(true, 0, input, 0, input.length());
  }

  /**
   * Returns alias suggestions for the given input.
   *
   * @param reader the input reader
   * @param contextSoFar the context, empty
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideAliasSuggestions(
          final StringReader reader, final CommandContextBuilder<S> contextSoFar) {
    final S source = contextSoFar.getSource();
    final String input = reader.getRead();

    final Collection<CommandNode<S>> aliases = this.root().getChildren();
    @SuppressWarnings("unchecked")
    final CompletableFuture<Suggestions>[] futures = new CompletableFuture[aliases.size()];
    int i = 0;
    for (final CommandNode<S> node : aliases) {
      CompletableFuture<Suggestions> future = Suggestions.empty();
      final String alias = node.getName();

      if (shouldConsider(alias, input) && node.canUse(source)) {
        final CommandContextBuilder<S> context = contextSoFar.copy();
        context.withNode(node, ALIAS_SUGGESTION_RANGE);
        if (node.canUse(context, reader)) {
          final SuggestionsBuilder builder = new SuggestionsBuilder(input, 0);
          future = builder.suggest(alias).buildFuture();
        }
      }
      futures[i++] = future;
    }

    return this.merge(input, futures);
  }

  /**
   * Merges the suggestions provided by the {@link Command} associated to the given
   * alias node and the hints given during registration for the given input.
   *
   * <p>The context is not mutated by this method. The reader's cursor may be modified.
   *
   * @param alias the alias node
   * @param reader the input reader
   * @param contextSoFar the context containing {@code alias}
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideArgumentsSuggestions(
          final LiteralCommandNode<S> alias, final StringReader reader,
          final CommandContextBuilder<S> contextSoFar) {
    final S source = contextSoFar.getSource();
    final GreedyArgumentCommandNode<S, ?> argsNode = VelocityCommands.getArgumentsNode(alias);
    if (argsNode == null) {
      // This is a BrigadierCommand, fallback to regular suggestions
      reader.setCursor(0);
      final ParseResults<S> parse = this.dispatcher.parse(reader, source);
      return this.dispatcher.getCompletionSuggestions(parse);
    }

    if (!argsNode.canUse(source)) {
      return Suggestions.empty();
    }

    final int start = reader.getCursor();
    final CommandContextBuilder<S> context = contextSoFar.copy();
    try {
      argsNode.parse(reader, context); // reads remaining input
    } catch (final CommandSyntaxException e) {
      throw new RuntimeException(e);
    }

    if (!argsNode.canUse(context, reader)) {
      return Suggestions.empty();
    }

    // Ask the command for suggestions via the arguments node
    reader.setCursor(start);
    final CompletableFuture<Suggestions> cmdSuggestions =
            this.getArgumentsNodeSuggestions(argsNode, reader, context);
    final boolean hasHints = alias.getChildren().size() > 1;
    if (!hasHints) {
      return cmdSuggestions;
    }

    // Parse the hint nodes to get remaining suggestions
    reader.setCursor(start);
    final CompletableFuture<Suggestions> hintSuggestions =
            this.getHintSuggestions(alias, reader, contextSoFar);

    final String fullInput = reader.getString();
    return this.merge(fullInput, cmdSuggestions, hintSuggestions);
  }

  /**
   * Returns the suggestions provided by the {@link Command} associated to the given
   * arguments node for the given input.
   *
   * <p>The reader and context are not mutated by this method.
   *
   * @param node the arguments node of the command
   * @param reader the input reader
   * @param context the context containing an alias node and {@code node}
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> getArgumentsNodeSuggestions(
          final GreedyArgumentCommandNode<S, ?> node, final StringReader reader,
          final CommandContextBuilder<S> context) {
    final int start = reader.getCursor();
    final String fullInput = reader.getString();
    final CommandContext<S> built = context.build(fullInput);
    try {
      return node.listSuggestions(built, new SuggestionsBuilder(fullInput, start));
    } catch (final CommandSyntaxException e) {
      LOGGER.error("Arguments node cannot provide suggestions, skipping", e);
      return Suggestions.empty();
    }
  }

  /**
   * Returns the suggestions provided by the matched hint nodes for the given input.
   *
   * <p>The reader and context are not mutated by this method.
   *
   * @param alias the alias node
   * @param reader the input reader
   * @param context the context object containing {@code alias}
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> getHintSuggestions(
          final LiteralCommandNode<S> alias, final StringReader reader,
          final CommandContextBuilder<S> context) {
    final ParseResults<S> parse = this.parseHints(alias, reader, context);
    return this.dispatcher.getCompletionSuggestions(parse);
  }

  /**
   * Parses the hint nodes under the given node, which is either an alias node of
   * a {@link Command} or another hint node.
   *
   * <p>The reader and context are not mutated by this method.
   *
   * @param node the node to parse
   * @param originalReader the input reader
   * @param contextSoFar the context containing the alias node of the command
   * @return the parse results containing the parsed hint nodes
   */
  private ParseResults<S> parseHints(final CommandNode<S> node, final StringReader originalReader,
                                     final CommandContextBuilder<S> contextSoFar) {
    // This is a stripped-down version of CommandDispatcher#parseNodes that doesn't
    // check the requirements are satisfied and ignores redirects, neither of
    // which are used by hint nodes. Parsing errors are ignored.
    List<ParseResults<S>> potentials = null;
    for (final CommandNode<S> child : node.getRelevantNodes(originalReader)) {
      if (VelocityCommands.isArgumentsNode(child)) {
        continue;
      }
      final CommandContextBuilder<S> context = contextSoFar.copy();
      final StringReader reader = new StringReader(originalReader);
      try {
        // We intentionally don't catch all unchecked exceptions
        child.parse(reader, context);
        if (reader.canRead() && reader.peek() != CommandDispatcher.ARGUMENT_SEPARATOR_CHAR) {
          continue;
        }
      } catch (final CommandSyntaxException e) {
        continue;
      }
      if (reader.canRead(2)) {
        reader.skip(); // separator
        final ParseResults<S> parse = this.parseHints(child, reader, context);
        if (potentials == null) {
          potentials = new ArrayList<>(1);
        }
        potentials.add(parse);
      } else {
        final ParseResults<S> parse =
                new ParseResults<>(context, reader, Collections.emptyMap());
        if (potentials == null) {
          potentials = new ArrayList<>(1);
        }
        potentials.add(parse);
      }
    }
    if (potentials != null) {
      if (potentials.size() > 1) {
        potentials.sort((a, b) -> {
          if (!a.getReader().canRead() && b.getReader().canRead()) {
            return -1;
          }
          if (a.getReader().canRead() && !b.getReader().canRead()) {
            return 1;
          }
          return 0;
        });
      }
      return potentials.get(0);
    }
    return new ParseResults<>(contextSoFar, originalReader, Collections.emptyMap());
  }

  /**
   * Returns a future that is completed with the result of merging the {@link Suggestions}
   * the given futures complete with. The results of the futures that complete exceptionally
   * are ignored.
   *
   * @param fullInput the command input
   * @param futures the futures that complete with the suggestions
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

  private RootCommandNode<S> root() {
    return this.dispatcher.getRoot();
  }
}
