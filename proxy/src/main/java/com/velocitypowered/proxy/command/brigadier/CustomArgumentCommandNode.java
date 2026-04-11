/*
 * Copyright (C) 2021-2023 Velocity Contributors
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
import com.mojang.brigadier.Command;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.CustomArgumentType;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * An argument node that uses the given (possibly custom) {@link ArgumentType} for parsing, while
 * maintaining compatibility with the vanilla client. The argument type must be greedy and accept
 * any input.
 *
 * @param <S> the type of the command source
 * @param <T> the type of the argument to parse
 */
public class CustomArgumentCommandNode<S, T, N> extends ArgumentCommandNode<S, N> {
  private final CustomArgumentType<T, N> type;

  CustomArgumentCommandNode(
          final String name,
          final CustomArgumentType<T, N> type,
          final Command<S> command,
          final Predicate<S> requirement,
          final BiPredicate<CommandContextBuilder<S>, ImmutableStringReader> contextRequirement,
          final CommandNode<S> redirect,
          final RedirectModifier<S> modifier,
          final boolean forks,
          final SuggestionProvider<S> customSuggestions) {
    super(name,
            type.getNativeType(),
            command,
            requirement,
            contextRequirement,
            redirect,
            modifier,
            forks,
            customSuggestions);
    this.type = Preconditions.checkNotNull(type, "type");
  }

  @Override
  public void parse(final StringReader reader, final CommandContextBuilder<S> contextBuilder)
      throws CommandSyntaxException {
    final int start = reader.getCursor();
    final T result = this.type.parse(reader);
    final ParsedArgument<S, T> parsed = new ParsedArgument<>(start, reader.getCursor(), result);

    contextBuilder.withArgument(getName(), parsed);
    contextBuilder.withNode(this, parsed.getRange());
  }

  @Override
  public CompletableFuture<Suggestions> listSuggestions(
      final CommandContext<S> context, final SuggestionsBuilder builder)
      throws CommandSyntaxException {
    if (getCustomSuggestions() == null) {
      return this.type.listSuggestions(context, builder);
    } else {
      return getCustomSuggestions().getSuggestions(context, builder);
    }
  }

  @Override
  public RequiredArgumentBuilder<S, N> createBuilder() {
    throw new UnsupportedOperationException();
  }

  /**
   * Creates a new builder from this instance.
   *
   * @return the builder
   */
  public CustomArgumentBuilder<S, T, N> createCustomArgBuilder() {
    return CustomArgumentBuilder.<S, T, N>argument(getName(), this.type)
            .suggests(getCustomSuggestions())
            .requires(getRequirement())
            .forward(getRedirect(), getRedirectModifier(), isFork())
            .executes(getCommand());
  }

  @Override
  public boolean isValidInput(String input) {
    try {
      final StringReader reader = new StringReader(input);
      this.type.parse(reader);
      return !reader.canRead() || reader.peek() == ' ';
    } catch (final CommandSyntaxException ignored) {
      return false;
    }
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof CustomArgumentCommandNode that)) {
      return false;
    }
    if (!super.equals(that)) {
      return false;
    }
    return this.type.equals(that.type);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + this.type.hashCode();
    return result;
  }

  @Override
  public Collection<String> getExamples() {
    return this.type.getExamples();
  }

  @Override
  public String toString() {
    return "<argument " + this.getName() + ":" + this.type + ">";
  }
}
