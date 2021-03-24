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

package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides utilities for working with Brigadier commands.
 */
public final class BrigadierUtils {

  private static final Splitter SPACE_SPLITTER = Splitter.on(' ');

  /**
   * Returns a literal node that redirects its execution to
   * the given destination node.
   *
   * @param alias the command alias
   * @param destination the destination node
   * @return the built node
   */
  public static LiteralCommandNode<CommandSource> buildRedirect(
          final String alias, final LiteralCommandNode<CommandSource> destination) {
    // Redirects only work for nodes with children, but break the top argument-less command.
    // Manually adding the root command after setting the redirect doesn't fix it.
    // See https://github.com/Mojang/brigadier/issues/46). Manually clone the node instead.
    LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder
            .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .requires(destination.getRequirement())
            .forward(
                destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
            .executes(destination.getCommand());
    for (CommandNode<CommandSource> child : destination.getChildren()) {
      builder.then(child);
    }
    return builder.build();
  }

  /**
   * Returns a literal node that optionally accepts arguments
   * as a raw {@link String}.
   *
   * @param alias the literal alias
   * @param brigadierCommand the command to execute
   * @param suggestionProvider the suggestion provider
   * @return the built node
   */
  public static LiteralCommandNode<CommandSource> buildRawArgumentsLiteral(
          final String alias, final Command<CommandSource> brigadierCommand,
          SuggestionProvider<CommandSource> suggestionProvider) {
    return LiteralArgumentBuilder
            .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .then(RequiredArgumentBuilder
                .<CommandSource, String>argument("arguments", StringArgumentType.greedyString())
                .suggests(suggestionProvider)
                .executes(brigadierCommand))
            .executes(brigadierCommand)
            .build();
  }

  /**
   * Returns the used command alias.
   *
   * @param context the command context
   * @return the parsed command alias
   */
  public static String getAlias(final CommandContext<CommandSource> context) {
    return context.getNodes().get(0).getNode().getName();
  }

  /**
   * Returns the raw {@link String} arguments of a command execution.
   *
   * @param context the command context
   * @return the parsed arguments
   */
  public static String getRawArguments(final CommandContext<CommandSource> context) {
    String cmdLine = context.getInput();
    int firstSpace = cmdLine.indexOf(' ');
    if (firstSpace == -1) {
      return "";
    }
    return cmdLine.substring(firstSpace + 1);
  }

  /**
   * Returns the splitted arguments of a command node built with
   * {@link #buildRawArgumentsLiteral(String, Command, SuggestionProvider)}.
   *
   * @param context the command context
   * @return the parsed arguments
   */
  public static String[] getSplitArguments(final CommandContext<CommandSource> context) {
    String line = getRawArguments(context);
    if (line.isEmpty()) {
      return new String[0];
    }
    return SPACE_SPLITTER.splitToList(line).toArray(new String[0]);
  }

  /**
   * Returns the normalized representation of the given command input.
   *
   * @param cmdLine the command input
   * @param trim whether to trim argument-less inputs
   * @return the normalized command
   */
  public static String normalizeInput(final String cmdLine, final boolean trim) {
    // Command aliases are case insensitive, but Brigadier isn't
    String command = trim ? cmdLine.trim() : cmdLine;
    int firstSpace = command.indexOf(' ');
    if (firstSpace != -1) {
      return command.substring(0, firstSpace).toLowerCase(Locale.ENGLISH)
              + command.substring(firstSpace);
    }
    return command.toLowerCase(Locale.ENGLISH);
  }

  /**
   * Prepares the given command node prior for hinting metadata to
   * a {@link com.velocitypowered.api.command.Command}.
   *
   * @param node the command node to be wrapped
   * @param command the command to execute
   * @return the wrapped command node
   */
  public static CommandNode<CommandSource> wrapForHinting(
      final CommandNode<CommandSource> node, final @Nullable Command<CommandSource> command) {
    Preconditions.checkNotNull(node, "node");
    ArgumentBuilder<CommandSource, ?> builder = node.createBuilder();
    builder.executes(command);
    for (CommandNode<CommandSource> child : node.getChildren()) {
      builder.then(wrapForHinting(child, command));
    }
    return builder.build();
  }

  private BrigadierUtils() {
    throw new AssertionError();
  }
}
