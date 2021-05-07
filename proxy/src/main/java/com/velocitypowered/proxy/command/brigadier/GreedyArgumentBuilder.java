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

package com.velocitypowered.proxy.command.brigadier;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;

/**
 * A builder for creating {@link GreedyArgumentCommandNode}s.
 *
 * @param <S> the type of the command source
 * @param <T> the type of the argument to parse
 */
public final class GreedyArgumentBuilder<S, T>
        extends ArgumentBuilder<S, GreedyArgumentBuilder<S, T>> {

  /**
   * Creates a {@link GreedyArgumentCommandNode} builder.
   *
   * @param name the node name
   * @param type the type of the argument
   * @param <S> the type of the command source
   * @param <T> the type of the argument to parse
   * @return a builder
   */
  public static <S, T> GreedyArgumentBuilder<S, T> greedyArgument(final String name,
                                                                  final ArgumentType<T> type) {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(type, "type");
    return new GreedyArgumentBuilder<>(name, type);
  }

  private final String name;
  private final ArgumentType<T> type;
  private SuggestionProvider<S> suggestionsProvider = null;

  private GreedyArgumentBuilder(final String name, final ArgumentType<T> type) {
    this.name = name;
    this.type = type;
  }

  public GreedyArgumentBuilder<S, T> suggests(final SuggestionProvider<S> provider) {
    this.suggestionsProvider = provider;
    return getThis();
  }

  @Override
  public GreedyArgumentBuilder<S, T> then(final ArgumentBuilder<S, ?> argument) {
    throw new UnsupportedOperationException("Cannot add children to a greedy node");
  }

  @Override
  public GreedyArgumentBuilder<S, T> then(final CommandNode<S> argument) {
    throw new UnsupportedOperationException("Cannot add children to a greedy node");
  }

  @Override
  protected GreedyArgumentBuilder<S, T> getThis() {
    return this;
  }

  @Override
  public GreedyArgumentCommandNode<S, T> build() {
    return new GreedyArgumentCommandNode<>(this.name, this.type, getCommand(), getRequirement(),
            getContextRequirement(), getRedirect(), getRedirectModifier(), isFork(),
            this.suggestionsProvider);
  }
}
