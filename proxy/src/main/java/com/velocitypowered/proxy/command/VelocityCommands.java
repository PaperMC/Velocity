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
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.RedirectModifier;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Provides parsing methods common to most {@link Command} implementations.
 * In particular, {@link InvocableCommand}s and legacy commands use the same logic for
 * creating and parsing the alias and arguments {@link CommandNode}s, which is contained
 * in this class.
 *
 * <p>A <i>fully-built</i> alias node is a node returned by
 * {@link CommandNodeFactory#create(Command, String)}.
 */
public final class VelocityCommands {

  static final String ARGS_NODE_NAME = "arguments";

  // Parsing

  /**
   * Returns the parsed alias, used to execute the command.
   *
   * @param context the context
   * @return the command alias
   */
  static String readAlias(final CommandContext<?> context) {
    if (context.getParent() != null) {
      // This is the child context of an alias redirect. Command implementations are interested
      // in knowing the used alias, i.e. the name of the first node in the root context.
      return readAlias(context.getParent());
    }
    return readAlias(context.getNodes());
  }

  /**
   * Returns the parsed alias, used to execute the command.
   *
   * @param context the context builder
   * @return the command alias
   */
  static String readAlias(final CommandContextBuilder<?> context) {
    if (context.getParent() != null) {
      return readAlias(context.getParent());
    }

    return readAlias(context.getNodes());
  }

  private static String readAlias(final List<? extends ParsedCommandNode<?>> nodes) {
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Cannot read alias from empty node list");
    }
    return nodes.get(0).getNode().getName();
  }

  /**
   * Returns the parsed arguments that come after the command alias, or {@code fallback} if
   * no arguments were provided.
   *
   * @param context the context
   * @param type the type class of the arguments
   * @param fallback the value to return if no arguments were provided
   * @param <V> the type of the arguments
   * @return the command arguments
   */
  static <V> V readArguments(final CommandContext<?> context,
                             final Class<V> type, final V fallback) {
    return readArguments(context.getArguments(), type, fallback);
  }

  /**
   * Returns the parsed arguments that come after the command alias, or {@code fallback} if
   * no arguments were provided.
   *
   * @param context the context builder
   * @param type the type class of the arguments
   * @param fallback the value to return if no arguments were provided
   * @param <V> the type of the arguments
   * @return the command arguments
   */
  static <V> V readArguments(final CommandContextBuilder<?> context,
                             final Class<V> type, final V fallback) {
    return readArguments(context.getArguments(), type, fallback);
  }

  private static <V> V readArguments(final Map<String, ? extends ParsedArgument<?, ?>> arguments,
                                     final Class<V> type, final V fallback) {
    final ParsedArgument<?, ?> argument = arguments.get(ARGS_NODE_NAME);
    if (argument == null) {
      return fallback;
    }
    final Object result = argument.getResult();
    try {
      return type.cast(result);
    } catch (final ClassCastException e) {
      throw new IllegalArgumentException("Parsed argument is of type " + result.getClass()
              + ", expected " + type, e);
    }
  }

  // Creation

  /**
   * Returns a node with the given alias that forwards its execution to the specified target node.
   *
   * @param target the target node
   * @param alias the case-insensitive alias
   * @return a literal node with a redirect to the given target node
   */
  static LiteralCommandNode<CommandSource> newAliasRedirect(
          final LiteralCommandNode<CommandSource> target, final String alias) {
    Preconditions.checkNotNull(target, "target");
    Preconditions.checkNotNull(alias, "alias");
    return LiteralArgumentBuilder
            .<CommandSource>literal(alias)
            .requires(target.getRequirement())
            .requiresWithContext(target.getContextRequirement())
            .redirect(target)
            .build();
  }

  /**
   * Returns an arguments node for the command represented by the given fully-built alias node.
   *
   * @param aliasNode the command alias node
   * @param type the type of the arguments
   * @param contextRequirement the arguments node context requirement
   * @param customSuggestions the suggestion provider for the arguments node
   * @param <V> the type of the arguments
   * @return the arguments node
   */
  static <V> ArgumentCommandNode<CommandSource, String> newArgumentsNode(
          final LiteralCommandNode<CommandSource> aliasNode, final ArgumentType<V> type,
          final BiPredicate<CommandContextBuilder<CommandSource>, ImmutableStringReader>
                  contextRequirement, final SuggestionProvider<CommandSource> customSuggestions) {
    return new ArgumentsCommandNode<>(type, aliasNode.getCommand(), source -> true,
            contextRequirement, customSuggestions);
  }

  /**
   * Returns whether the given node is an arguments node.
   *
   * @param node the node to check
   * @return {@code true} if the node is an arguments node
   * @see #newArgumentsNode(LiteralCommandNode, ArgumentType, BiPredicate, SuggestionProvider) to
   *      create an arguments node
   */
  public static boolean isArgumentsNode(final CommandNode<?> node) {
    return node instanceof ArgumentsCommandNode;
  }

  // The Vanilla client doesn't know how to serialize the ArgumentTypes used by the invocation
  // factories. The data sent to the client look like it comes from a StringArgumentType, but
  // is generated by the given parsingType.
  private static final class ArgumentsCommandNode<T>
          extends ArgumentCommandNode<CommandSource, String> {

    private final ArgumentType<T> parsingType;

    ArgumentsCommandNode(
            final ArgumentType<T> parsingType,
            final com.mojang.brigadier.Command<CommandSource> command,
            final Predicate<CommandSource> requirement,
            final BiPredicate<CommandContextBuilder<CommandSource>, ImmutableStringReader>
                    contextRequirement, final SuggestionProvider<CommandSource> customSuggestions) {
      super(ARGS_NODE_NAME, StringArgumentType.greedyString(), command, requirement,
              contextRequirement, null, null, false,
              Preconditions.checkNotNull(customSuggestions, "customSuggestions"));
      this.parsingType = Preconditions.checkNotNull(parsingType, "parsingType");
    }

    @Override
    public void parse(final StringReader reader,
                      final CommandContextBuilder<CommandSource> contextBuilder)
            throws CommandSyntaxException {
      // Same as overridden method, except this uses the parsingType
      final int start = reader.getCursor();
      final T result = this.parsingType.parse(reader);
      final ParsedArgument<CommandSource, T> parsed =
              new ParsedArgument<>(start, reader.getCursor(), result);

      contextBuilder.withArgument(getName(), parsed);
      contextBuilder.withNode(this, parsed.getRange());
    }

    @Override
    public CompletableFuture<Suggestions> listSuggestions(
            final CommandContext<CommandSource> context, final SuggestionsBuilder builder)
            throws CommandSyntaxException {
      return getCustomSuggestions().getSuggestions(context, builder);
    }

    @Override
    public boolean isValidInput(final String input) {
      return true; // all strings are valid arguments
    }

    @Override
    public RequiredArgumentBuilder<CommandSource, String> createBuilder() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void addChild(final CommandNode<CommandSource> node) {
      throw new UnsupportedOperationException("Cannot add children to arguments node");
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      final ArgumentsCommandNode<?> that = (ArgumentsCommandNode<?>) o;
      return parsingType.equals(that.parsingType);
    }

    @Override
    public int hashCode() {
      int result = super.hashCode();
      result = 31 * result + parsingType.hashCode();
      return result;
    }
  }

  // Hinting

  /**
   * Returns a node to use for hinting the arguments of a command. Hint nodes are sent to
   * 1.13+ clients, and the proxy uses them for providing suggestions.
   *
   * <p>A hint node is able to provide suggestions if and only if the requirements of
   * the corresponding arguments node pass.
   *
   * @param hint the node containing hinting metadata
   * @return the hinting command node
   * @throws IllegalArgumentException if the hinting node is executable or has a redirect
   */
  static CommandNode<CommandSource> newHintingNode(final CommandNode<CommandSource> hint) {
    Preconditions.checkNotNull(hint, "hint");
    if (hint.getCommand() != null) {
      throw new IllegalArgumentException("Cannot use an executable node for hinting");
    }
    if (hint.getRedirect() != null) {
      throw new IllegalArgumentException("Cannot use a node with a redirect for hinting");
    }
    ArgumentBuilder<CommandSource, ?> builder = hint.createBuilder()
            // Requirement checking is performed by SuggestionsProvider
            .requires(source -> false);
    for (final CommandNode<CommandSource> child : hint.getChildren()) {
      builder.then(newHintingNode(child));
    }
    return builder.build();
  }

  private VelocityCommands() {
    throw new AssertionError();
  }
}
