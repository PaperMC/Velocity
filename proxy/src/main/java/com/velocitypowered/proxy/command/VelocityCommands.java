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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.proxy.command.brigadier.GreedyArgumentCommandNode;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides parsing methods common to most {@link Command} implementations.
 * In particular, {@link InvocableCommand}s and legacy commands use the same logic for
 * creating and parsing the alias and arguments {@link CommandNode}s, which is contained
 * in this class.
 */
public final class VelocityCommands {

  public static final String ARGS_NODE_NAME = "arguments";

  // Parsing

  /**
   * Returns the parsed alias, used to execute the command.
   *
   * @param context the context
   * @return the command alias
   */
  public static String readAlias(final CommandContext<?> context) {
    return readAlias(context.getNodes());
  }

  /**
   * Returns the parsed alias, used to execute the command.
   *
   * @param context the context builder
   * @return the command alias
   */
  public static String readAlias(final CommandContextBuilder<?> context) {
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
  public static <V> V readArguments(final CommandContext<?> context,
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
  public static <V> V readArguments(final CommandContextBuilder<?> context,
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

  /**
   * Returns the arguments node for the command represented by the given alias node,
   * if present; otherwise returns {@code null}.
   *
   * @param alias the alias node
   * @param <S> the type of the command source
   * @return the arguments node, or {@code null} if not present
   */
  static <S> @Nullable GreedyArgumentCommandNode<S, ?> getArgumentsNode(
          final LiteralCommandNode<S> alias) {
    final CommandNode<S> node = alias.getChild(ARGS_NODE_NAME);
    if (node instanceof GreedyArgumentCommandNode) {
      return (GreedyArgumentCommandNode<S, ?>) node;
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
    return node instanceof GreedyArgumentCommandNode && node.getName().equals(ARGS_NODE_NAME);
  }

  private VelocityCommands() {
    throw new AssertionError();
  }
}
