package com.velocitypowered.proxy.util;

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

  /**
   * The return code used by wrapped commands to signal the given
   * {@link CommandSource} doesn't have permission to run the command.
   */
  public static final int NO_PERMISSION = 0xF6287429;

  /**
   * Returns a literal node that redirects its execution to
   * the given destination node.
   *
   * @param alias the command alias
   * @param destination the destination node
   * @return the built node
   */
  public static LiteralCommandNode<CommandSource> buildRedirect(
          final String alias, final CommandNode<CommandSource> destination) {
    LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder
            .literal(alias.toLowerCase(Locale.ENGLISH));

    if (!destination.getChildren().isEmpty()) {
      builder.redirect(destination);
    } else {
      // Redirects don't work for nodes without children (argument-less commands).
      // See https://github.com/Mojang/brigadier/issues/46).
      // Manually construct redirect instead (LiteralCommandNode.createBuilder)
      builder.requires(destination.getRequirement());
      builder.forward(
              destination.getRedirect(), destination.getRedirectModifier(), destination.isFork());
      builder.executes(destination.getCommand());
    }
    return builder.build();
  }

  private static final String ARGUMENTS_NAME = "arguments";

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
    LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .executes(brigadierCommand)
            .build();
    CommandNode<CommandSource> arguments = RequiredArgumentBuilder
            .<CommandSource, String>argument(ARGUMENTS_NAME, StringArgumentType.greedyString())
            .suggests(suggestionProvider)
            .executes(brigadierCommand)
            .build();
    node.addChild(arguments);
    return node;
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
   * Returns the raw {@link String} arguments of a command node built with
   * {@link #buildRawArgumentsLiteral(String, Command, SuggestionProvider)}.
   *
   * @param context the command context
   * @return the parsed arguments
   */
  public static String getRawArguments(final CommandContext<CommandSource> context) {
    if (context.getNodes().size() == 1) {
      return ""; // only the command alias was passed
    }
    return context.getArgument(ARGUMENTS_NAME, String.class);
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
    return line.trim().split(" ", -1);
  }

  private BrigadierUtils() {
    throw new AssertionError();
  }
}
