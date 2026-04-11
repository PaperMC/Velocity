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
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.velocitypowered.api.command.CustomArgumentType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A builder for creating {@link ArgumentCommandNode}s.
 *
 * @param <S> the type of the command source
 * @param <T> the custom type of the argument to parse
 * @param <N> the native type of the argument to parse
 */
public final class CustomArgumentBuilder<S, T, N> extends ArgumentBuilder<S, CustomArgumentBuilder<S, T, N>> {

  /**
   * Creates a builder for creating {@link ArgumentCommandNode}s with the given name and
   * type.
   *
   * @param name         the name of the node
   * @param argumentType the type of the argument to parse
   * @param <S>          the type of the command source
   * @param <T>          the type of the custom argument to parse
   * @param <N>          the type of the native argument to parse
   * @return a builder
   */
  public static <S, T, N> CustomArgumentBuilder<S, T, N> argument(final String name, final CustomArgumentType<T, N> argumentType) {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(argumentType, "argument type");
    return new CustomArgumentBuilder<>(name, argumentType);
  }

  private final String name;
  private final CustomArgumentType<T, N> type;
  private SuggestionProvider<S> suggestionsProvider = null;

  private CustomArgumentBuilder(final String name, final CustomArgumentType<T, N> type) {
    this.name = name;
    this.type = type;
  }

  public CustomArgumentType<T, N> getType() {
    return this.type;
  }

  public String getName() {
    return this.name;
  }

  public CustomArgumentBuilder<S, T, N> suggests(final @Nullable SuggestionProvider<S> provider) {
    this.suggestionsProvider = provider;
    return this;
  }

  public SuggestionProvider<S> getSuggestionsProvider() {
    return suggestionsProvider;
  }

  @Override
  protected CustomArgumentBuilder<S, T, N> getThis() {
    return this;
  }

  @Override
  public CustomArgumentCommandNode<S, T, N> build() {
    return new CustomArgumentCommandNode<>(this.name,
            this.type,
            getCommand(),
            getRequirement(),
            getContextRequirement(),
            getRedirect(),
            getRedirectModifier(),
            isFork(),
            this.suggestionsProvider);
  }
}