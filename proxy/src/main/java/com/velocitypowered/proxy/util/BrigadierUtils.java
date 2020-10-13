package com.velocitypowered.proxy.util;

import com.google.common.base.Splitter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import java.util.Locale;

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
   * Returns the command name from the specified input.
   *
   * @param input command input
   * @return command name from the specified input
   */
  public static String getCommandName(final String input) {
    int firstSpace = input.indexOf(' ');
    if (firstSpace == -1) {
      return input;
    }
    return input.substring(0, firstSpace);
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

  private BrigadierUtils() {
    throw new AssertionError();
  }
}
