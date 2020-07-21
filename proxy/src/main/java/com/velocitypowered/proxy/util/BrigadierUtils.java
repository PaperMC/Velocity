package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.CommandSource;
import java.util.Locale;
import java.util.function.Predicate;

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
    return LiteralArgumentBuilder
            .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
            .then(RequiredArgumentBuilder
                .<CommandSource, String>argument(ARGUMENTS_NAME, StringArgumentType.greedyString())
                .suggests(suggestionProvider)
                .executes(brigadierCommand)
            )
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
   * Returns a node whose commands are executed iff the given predicate passes.
   * Otherwise, the command returns {@code predicateFailReturn}.
   *
   * @param node the node to wrap
   * @param predicate the predicate to test before command execution
   * @param predicateFailReturn the execution return if the predicate fails
   * @return the wrapped node
   */
  public static CommandNode<CommandSource> wrapWithContextPredicate(
          final CommandNode<CommandSource> node,
          final Predicate<CommandContext<CommandSource>> predicate, final int predicateFailReturn) {
    Preconditions.checkArgument(node.getRedirect() == null, "cannot wrap redirect");
    ArgumentBuilder<CommandSource, ?> builder = node.createBuilder();
    if (node.getCommand() != null) {
      builder.executes(context -> {
        if (!predicate.test(context)) {
          return predicateFailReturn;
        }
        return node.getCommand().run(context);
      });
    }
    if (node instanceof ArgumentCommandNode) {
      SuggestionProvider<CommandSource> suggestionProvider =
              ((ArgumentCommandNode<CommandSource, ?>) node).getCustomSuggestions();
      //noinspection unchecked
      ((RequiredArgumentBuilder<CommandSource, ?>) builder).suggests((context, builder1) -> {
        if (!predicate.test(context)) {
          return Suggestions.empty();
        }
        return suggestionProvider.getSuggestions(context, builder1);
      });
    }
    for (CommandNode<CommandSource> child : node.getChildren()) {
      builder.then(wrapWithContextPredicate(child, predicate, predicateFailReturn));
    }
    return builder.build();
  }

  private BrigadierUtils() {
    throw new AssertionError();
  }
}
