package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides parsing methods common to most {@link Command} implementations.
 * In particular, {@link InvocableCommand}s and legacy commands use the same logic for
 * creating and parsing the alias and arguments {@link CommandNode}s, which is contained
 * in this class.
 *
 * <p>A <i>fully-built</i> alias node is a node returned by
 * {@link CommandNodeFactory#create(Command, String)}.
 */
final class VelocityCommands {

  private static final String ARGS_NODE_NAME = "arguments";

  // Parsing

  /**
   * Parses the alias used to execute the command.
   *
   * @param context the command context
   * @return the case-sensitive alias
   */
  static String readAlias(final CommandContext<CommandSource> context) {
    if (!context.hasNodes()) {
      throw new IllegalArgumentException("Cannot read alias from childless root node");
    }
    return context.getNodes().get(0).getNode().getName();
  }

  /**
   * Returns the parsed alias, used to execute the command.
   *
   * @param parse the parse results
   * @return the case-sensitive alias
   */
  static String readAlias(final ParseResults<CommandSource> parse) {
    final List<ParsedCommandNode<CommandSource>> nodes = parse.getContext().getNodes();
    if (nodes.isEmpty()) {
      throw new IllegalArgumentException("Cannot read alias from childless root node");
    }
    return nodes.get(0).getNode().getName();
  }

  /**
   * Parses the arguments after the command alias, or {@code fallback} if no arguments
   * were provided.
   *
   * @param context the command context
   * @param type the type class of the arguments
   * @param fallback the value to return if no arguments were provided
   * @param <V> the type of the arguments
   * @return the arguments
   * @see #argumentBuilder(ArgumentType, LiteralCommandNode) for creating arguments nodes
   */
  static <V> V readArguments(final CommandContext<CommandSource> context,
                             final Class<V> type, final V fallback) {
    return readArguments(context.getArguments(), type, fallback);
  }

  /**
   * Returns the parsed arguments that come after the command alias, or {@code fallback} if
   * no arguments were provided.
   *
   * @param parse the parse results
   * @param type the type class of the arguments
   * @param fallback the value to return if no arguments were provided
   * @param <V> the type of the arguments
   * @return the arguments
   * @see #argumentBuilder(ArgumentType, LiteralCommandNode) for creating arguments nodes
   */
  static <V> V readArguments(final ParseResults<CommandSource> parse,
                             final Class<V> type, final V fallback) {
    return readArguments(parse.getContext().getArguments(), type, fallback);
  }

  private static <V> V readArguments(final Map<String, ParsedArgument<CommandSource, ?>> arguments,
                            final Class<V> type, final V fallback) {
    final ParsedArgument<CommandSource, ?> argument = arguments.get(ARGS_NODE_NAME);
    if (argument == null) {
      return fallback;
    }
    final Object value = argument.getResult();
    try {
      return type.cast(value);
    } catch (final ClassCastException e) {
      throw new IllegalArgumentException("Parsed argument is of type " + value.getClass()
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
  static LiteralCommandNode<CommandSource> createAliasRedirect(
          final LiteralCommandNode<CommandSource> target, final String alias) {
    Preconditions.checkNotNull(target, "target");
    Preconditions.checkNotNull(alias, "alias");
    return LiteralArgumentBuilder
            .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .redirect(target)
            .build();
  }

  /**
   * Returns an arguments node builder for a command represented by the given fully-built
   * alias node.
   *
   * @param type the type class of the arguments
   * @param aliasNode the command alias node
   * @param <V> the type of the arguments
   * @return the arguments node builder
   */
  static <V> RequiredArgumentBuilder<CommandSource, V> argumentBuilder(
          final ArgumentType<V> type, final LiteralCommandNode<CommandSource> aliasNode) {
    Preconditions.checkNotNull(type, "type");
    Preconditions.checkNotNull(aliasNode, "aliasNode");
    return RequiredArgumentBuilder
            .<CommandSource, V>argument(ARGS_NODE_NAME, type)
            .requires(aliasNode.getRequirement())
            .requiresWithContext(aliasNode.getContextRequirement())
            .executes(aliasNode.getCommand());
  }

  // Hinting

  /**
   * Returns a node to use for hinting the command represented by the given fully-built alias node.
   * The hinting metadata is contained in the given {@link CommandNode}.
   *
   * @param aliasNode the alias node of the command to hint
   * @param hint the node containing hinting metadata
   * @return the hinting command node
   * @throws IllegalArgumentException if the given hinting node is executable or has a redirect
   */
  static CommandNode<CommandSource> createHintingNode(
          final LiteralCommandNode<CommandSource> aliasNode,
          final CommandNode<CommandSource> hint) {
    Preconditions.checkNotNull(aliasNode, "aliasNode");
    Preconditions.checkNotNull(hint, "hint");
    if (hint.getCommand() != null) {
      throw new IllegalArgumentException("Cannot use an executable node for hinting");
    }
    if (hint.getRedirect() != null) {
      throw new IllegalArgumentException("Cannot use a node with a redirect for hinting");
    }
    ArgumentBuilder<CommandSource, ?> builder = hint.createBuilder()
            .requires(aliasNode.getRequirement())
            .requiresWithContext(aliasNode.getContextRequirement())
            .executes(aliasNode.getCommand());
    for (final CommandNode<CommandSource> child : hint.getChildren()) {
      builder.then(createHintingNode(aliasNode, child));
    }
    return builder.build();
  }

  private VelocityCommands() {
    throw new AssertionError();
  }
}
