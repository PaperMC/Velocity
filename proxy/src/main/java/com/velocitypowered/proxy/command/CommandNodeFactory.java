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
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Locale;
import java.util.Map;

interface CommandNodeFactory<T extends Command> {

  CommandNodeFactory<SimpleCommand> SIMPLE = new InvocableCommandNodeFactory<>(
                  SimpleCommandInvocation.FACTORY, StringArrayArgumentType.INSTANCE);

  CommandNodeFactory<RawCommand> RAW = new InvocableCommandNodeFactory<>(
                  RawCommandInvocation.FACTORY, StringArgumentType.greedyString());

  LegacyCommandNodeFactory LEGACY = new LegacyCommandNodeFactory();

  /*
    private static void checkValidForHinting(final CommandNode<?> node) {
    if (node.getCommand() != null) {
      throw new IllegalArgumentException("Hinting node may not contain a Command");
    }
    if (node.getRedirect() != null) {
      throw new IllegalArgumentException("Hinting node may not be a redirect");
    }
    if (node.isFork()) {
      throw new IllegalArgumentException("Hinting node may not fork");
    }
    for (CommandNode<?> child : node.getChildren()) {
      checkValidForHinting(child);
    }
  }
   */

  static String readAlias(final CommandContext<?> context) {
    if (!context.hasNodes()) {
      throw new IllegalArgumentException("Context root node has no children");
    }
    return context.getNodes().get(0).getNode().getName();
  }

  static String readAlias(final ParseResults<?> parse) {
    if (parse.getContext().getNodes().isEmpty()) {
      throw new IllegalArgumentException("Parsed context root node has no children");
    }
    return parse.getContext().getNodes().get(0).getNode().getName();
  }

  String ARGS_NODE_NAME = "arguments";

  static <V> V readArguments(final CommandContext<CommandSource> context, final Class<V> type,
                             final V fallback) {
    return readArguments(context.getArguments(), type, fallback);
  }

  static <V> V readArguments(final ParseResults<CommandSource> parse, final Class<V> type,
                             final V fallback) {
    return readArguments(parse.getContext().getArguments(), type, fallback);
  }

  @SuppressWarnings("unchecked")
  static <V> V readArguments(final Map<String, ParsedArgument<CommandSource, ?>> arguments,
                             final Class<V> type, final V fallback) {
    final ParsedArgument<?, ?> argument = arguments.get(ARGS_NODE_NAME);
    if (argument == null) {
      return fallback;
    }
    final Object value = argument.getResult();
    if (!type.isAssignableFrom(value.getClass())) {
      throw new IllegalArgumentException("Arguments node type is " + value.getClass()
              + ", expected " + type);
    }
    return (V) value;
  }

  static <V> RequiredArgumentBuilder<CommandSource, V> argumentBuilder(
          final ArgumentType<V> type, final LiteralCommandNode<CommandSource> aliasNode) {
    return RequiredArgumentBuilder
            .<CommandSource, V>argument(ARGS_NODE_NAME, type)
            .requires(aliasNode.getRequirement())
            .requiresWithContext(aliasNode.getContextRequirement())
            .executes(aliasNode.getCommand());
  }

  LiteralCommandNode<CommandSource> create(final T command, final String alias);

  class InvocableCommandNodeFactory
          <T extends InvocableCommand<I>, I extends CommandInvocation<A>, A>
          implements CommandNodeFactory<T> {

    private final CommandInvocationFactory<I> invocationFactory;
    private final ArgumentType<A> argumentsType;

    public InvocableCommandNodeFactory(final CommandInvocationFactory<I> invocationFactory,
                                       final ArgumentType<A> argumentsType) {
      this.invocationFactory = Preconditions.checkNotNull(invocationFactory);
      this.argumentsType = Preconditions.checkNotNull(argumentsType);
    }

    @Override
    public LiteralCommandNode<CommandSource> create(final T command, final String alias) {
      Preconditions.checkNotNull(command, "command");
      final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
              .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
              .requiresWithContext(parse -> {
                final I invocation = invocationFactory.create(parse);
                return command.hasPermission(invocation);
              })
              .executes(context -> {
                final I invocation = invocationFactory.create(context);
                command.execute(invocation);
                return 1;
              })
              .build();

      final ArgumentCommandNode<CommandSource, A> argumentsNode =
              argumentBuilder(argumentsType, node)
                .suggests((context, builder) -> {
                  final I invocation = invocationFactory.create(context);
                  return command.suggestAsync(invocation).thenApply(suggestions -> {
                    for (String value : suggestions) {
                      builder.suggest(Preconditions.checkNotNull(value, "suggestion"));
                    }
                    return builder.build();
                  });
                })
                .build();
      node.addChild(argumentsNode);

      // TODO Hinting
      return node;
    }
  }

  final class LegacyCommandNodeFactory implements CommandNodeFactory<Command> {

    @Override
    public LiteralCommandNode<CommandSource> create(final Command command, final String alias) {
      final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
              .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
              .requiresWithContext(parse -> {
                final String[] args = readArguments(
                        parse, String[].class, StringArrayArgumentType.EMPTY);
                return command.hasPermission(parse.getContext().getSource(), args);
              })
              .executes(context -> {
                final String[] args = readArguments(
                        context, String[].class, StringArrayArgumentType.EMPTY);
                command.execute(context.getSource(), args);
                return 1;
              })
              .build();

      final ArgumentCommandNode<CommandSource, String[]> argumentsNode =
              argumentBuilder(StringArrayArgumentType.INSTANCE, node)
                .suggests((context, builder) -> {
                  final String[] args = readArguments(
                          context, String[].class, StringArrayArgumentType.EMPTY);
                  return command.suggestAsync(context.getSource(), args).thenApply(suggestions -> {
                    for (String value : suggestions) {
                      builder.suggest(Preconditions.checkNotNull(value, "suggestion"));
                    }
                    return builder.build();
                  });
                })
                .build();
      node.addChild(argumentsNode);
      // Legacy commands don't support hinting
      return node;
    }
  }
}
