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
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Provides suggestions for a given command input.
 *
 * <p>Similar to {@link CommandDispatcher#getCompletionSuggestions(ParseResults)}, except it
 * avoids fully parsing the given input and performs exactly one requirement predicate check
 * per considered node.
 *
 * @param <S> the type of the command source
 */
final class SuggestionsProvider<S> {

  private final CommandDispatcher<S> dispatcher;

  SuggestionsProvider(final CommandDispatcher<S> dispatcher) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
  }

  /**
   * Provides suggestions for the given input and source.
   *
   * @param input the partial input
   * @param source the command source invoking the command
   * @return a future that completes with the suggestions
   */
  public CompletableFuture<Suggestions> provideSuggestions(final String input, final S source) {
    final CommandContextBuilder<S> context = new CommandContextBuilder<>(
            this.dispatcher, source, this.dispatcher.getRoot(), 0);
    return this.provideSuggestions(new StringReader(input), context);
  }

  /**
   * Provides suggestions for the given input and context.
   *
   * @param reader the input reader
   * @param context an empty context
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideSuggestions(
          final StringReader reader, final CommandContextBuilder<S> context) {
    final StringRange aliasRange = this.consumeAlias(reader);
    final String alias = aliasRange.get(reader); // TODO #toLowerCase and test
    final LiteralCommandNode<S> literal =
            (LiteralCommandNode<S>) context.getRootNode().getChild(alias);

    final boolean hasArguments = reader.canRead();
    if (hasArguments) {
      if (literal == null) {
        // Input has arguments for non-registered alias
        return Suggestions.empty();
      }
      context.withNode(literal, aliasRange);
      reader.skip(); // separator
      // TODO Provide arguments suggestions
    } else {
      // TODO Provide alias suggestions
    }
  }

  private StringRange consumeAlias(final StringReader reader) {
    final int firstSep = reader.getString().indexOf(
            CommandDispatcher.ARGUMENT_SEPARATOR_CHAR, reader.getCursor());
    final StringRange range = StringRange.between(
            reader.getCursor(), firstSep == -1 ? reader.getTotalLength() : firstSep);
    reader.setCursor(range.getEnd());
    return range;
  }

  /**
   * Returns alias suggestions for the given input.
   *
   * @param reader the input reader
   * @param contextSoFar an empty context
   * @return a future that completes with the suggestions
   */
  private CompletableFuture<Suggestions> provideAliasSuggestions(
          final StringReader reader, final CommandContextBuilder<S> contextSoFar) {
    final S source = contextSoFar.getSource();
    final String input = reader.getRead();

    final Collection<CommandNode<S>> aliases = contextSoFar.getRootNode().getChildren();
    final CompletableFuture<Suggestions>[] futures = new CompletableFuture[aliases.size()];

  }

  /*private RootCommandNode<S> getRoot() {
    return this.dispatcher.getRoot();
  }*/
}
