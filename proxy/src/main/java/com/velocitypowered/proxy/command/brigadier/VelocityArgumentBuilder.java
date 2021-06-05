package com.velocitypowered.proxy.command.brigadier;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A builder for creating {@link VelocityArgumentCommandNode}s.
 *
 * @param <S> the type of the command source
 * @param <T> the type of the argument to parse
 */
public final class VelocityArgumentBuilder<S, T>
        extends ArgumentBuilder<S, VelocityArgumentBuilder<S, T>> {

  public static <S, T> VelocityArgumentBuilder<S, T> velocityArgument(final String name,
                                                                    final ArgumentType<T> type) {
    Preconditions.checkNotNull(name, "name");
    Preconditions.checkNotNull(type, "type");
    return new VelocityArgumentBuilder<>(name, type);
  }

  private final String name;
  private final ArgumentType<T> type;
  private SuggestionProvider<S> suggestionsProvider = null;

  public VelocityArgumentBuilder(final String name, final ArgumentType<T> type) {
    this.name = name;
    this.type = type;
  }

  public VelocityArgumentBuilder<S, T> suggests(final @Nullable SuggestionProvider<S> provider) {
    this.suggestionsProvider = provider;
    return this;
  }

  @Override
  public VelocityArgumentBuilder<S, T> then(final ArgumentBuilder<S, ?> argument) {
    throw new UnsupportedOperationException("Cannot add children to a greedy node");
  }

  @Override
  public VelocityArgumentBuilder<S, T> then(final CommandNode<S> argument) {
    throw new UnsupportedOperationException("Cannot add children to a greedy node");
  }

  @Override
  protected VelocityArgumentBuilder<S, T> getThis() {
    return this;
  }

  @Override
  public VelocityArgumentCommandNode<S, T> build() {
    return new VelocityArgumentCommandNode<>(this.name, this.type, getCommand(), getRequirement(),
            getContextRequirement(), getRedirect(), getRedirectModifier(), isFork(),
            this.suggestionsProvider);
  }
}
