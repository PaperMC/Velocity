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

package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.proxy.command.brigadier.VelocityArgumentCommandNode;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides utility methods common to most {@link Command} implementations. In particular,
 * {@link InvocableCommand} implementations use the same logic for creating and parsing the alias
 * and arguments command nodes, which is contained in this class.
 */
public final class VelocityCommands {

  // Normalization

  /**
   * Normalizes the given command input.
   *
   * @param input the raw command input, without the leading slash ('/')
   * @param trim  whether to remove leading and trailing whitespace from the input
   * @return the normalized command input
   */
  static String normalizeInput(final String input, final boolean trim) {
    final String command = trim ? input.trim() : input;
    int firstSep = command.indexOf(CommandDispatcher.ARGUMENT_SEPARATOR_CHAR);
    if (firstSep != -1) {
      // Aliases are case-insensitive, arguments are not
      return command.substring(0, firstSep).toLowerCase(Locale.ENGLISH)
          + command.substring(firstSep);
    } else {
      return command.toLowerCase(Locale.ENGLISH);
    }
  }

  // Parsing

  /**
   * Returns the parsed alias, used to execute the command.
   *
   * @param nodes the list of parsed nodes, as returned by {@link CommandContext#getNodes()} or
   *              {@link CommandContextBuilder#getNodes()}
   * @return the command alias
   */
  public static String readAlias(final List<? extends ParsedCommandNode<?>> nodes) {
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Cannot read alias from empty node list");
    }
    return nodes.get(0).getNode().getName();
  }

  public static final String ARGS_NODE_NAME = "arguments";

  /**
   * Returns the parsed arguments that come after the command alias, or {@code fallback} if no
   * arguments were provided.
   *
   * @param arguments the map of parsed arguments, as returned by
   *                  {@link CommandContext#getArguments()} or
   *                  {@link CommandContextBuilder#getArguments()}
   * @param type      the type class of the arguments
   * @param fallback  the value to return if no arguments were provided
   * @param <V>       the type of the arguments
   * @return the command arguments
   */
  public static <V> V readArguments(final Map<String, ? extends ParsedArgument<?, ?>> arguments,
      final Class<V> type, final V fallback) {
    final ParsedArgument<?, ?> argument = arguments.get(ARGS_NODE_NAME);
    if (argument == null) {
      return fallback; // either no arguments were given or this isn't an InvocableCommand
    }
    final Object result = argument.getResult();
    try {
      return type.cast(result);
    } catch (final ClassCastException e) {
      throw new IllegalArgumentException("Parsed argument is of type " + result.getClass()
          + ", expected " + type, e);
    }
  }

  // Alias nodes

  /**
   * Returns whether a literal node with the given name can be added to the {@link RootCommandNode}
   * associated to a {@link CommandManager}.
   *
   * <p>This is an internal method and should not be used in user-facing
   * methods. Instead, they should lowercase the given aliases themselves.
   *
   * @param alias the alias to check
   * @return true if the alias can be registered; false otherwise
   */
  public static boolean isValidAlias(final String alias) {
    return alias.equals(alias.toLowerCase(Locale.ENGLISH));
  }

  /**
   * Creates a copy of the given literal with the specified name.
   *
   * @param original the literal node to copy
   * @param newName  the name of the returned literal node
   * @return a copy of the literal with the given name
   */
  public static LiteralCommandNode<CommandSource> shallowCopy(
      final LiteralCommandNode<CommandSource> original, final String newName) {
    // Brigadier resolves the redirect of a node if further input can be parsed.
    // Let <bar> be a literal node having a redirect to a <foo> literal. Then,
    // the context returned by CommandDispatcher#parseNodes when given the input
    // string "<bar> " does not contain a child context with <foo> as its root node.
    // Thus, the vanilla client asks the children of <bar> for suggestions, instead
    // of those of <foo> (https://github.com/Mojang/brigadier/issues/46).
    // Perform a shallow copy of the literal instead.
    Preconditions.checkNotNull(original, "original");
    Preconditions.checkNotNull(newName, "secondaryAlias");
    final LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder
        .<CommandSource>literal(newName)
        .requires(original.getRequirement())
        .requiresWithContext(original.getContextRequirement())
        .forward(original.getRedirect(), original.getRedirectModifier(), original.isFork())
        .executes(original.getCommand());
    for (final CommandNode<CommandSource> child : original.getChildren()) {
      builder.then(child);
    }
    return builder.build();
  }

  // Arguments node

  /**
   * Returns the arguments node for the command represented by the given alias node, if present;
   * otherwise returns {@code null}.
   *
   * @param alias the alias node
   * @param <S>   the type of the command source
   * @return the arguments node, or null if not present
   */
  static <S> @Nullable VelocityArgumentCommandNode<S, ?> getArgumentsNode(
      final LiteralCommandNode<S> alias) {
    final CommandNode<S> node = alias.getChild(ARGS_NODE_NAME);
    if (node instanceof VelocityArgumentCommandNode) {
      return (VelocityArgumentCommandNode<S, ?>) node;
    }
    return null;
  }

  /**
   * Returns whether the given node is an arguments node.
   *
   * @param node the node to check
   * @return true if the node is an arguments node; false otherwise
   */
  public static boolean isArgumentsNode(final CommandNode<?> node) {
    return node instanceof VelocityArgumentCommandNode && node.getName().equals(ARGS_NODE_NAME);
  }

  private VelocityCommands() {
    throw new AssertionError();
  }
}
