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
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.api.command.SimpleCommand;
import java.util.Locale;

/**
 * Constructs the {@link LiteralCommandNode} representation of a given {@link Command}.
 *
 * <p>The alias and arguments of a command invocation involving the literal node returned
 * by {@link #create(Command, String)} can be retrieved using the utility methods in
 * the {@link VelocityCommands} class.
 *
 * @param <T> the type of the command
 */
interface CommandNodeFactory<T extends Command> {

  CommandNodeFactory<SimpleCommand> SIMPLE = new InvocableCommandNodeFactory<>(
                  SimpleCommandInvocation.FACTORY, StringArrayArgumentType.INSTANCE);

  CommandNodeFactory<RawCommand> RAW = new InvocableCommandNodeFactory<>(
                  RawCommandInvocation.FACTORY, StringArgumentType.greedyString());

  LegacyCommandNodeFactory LEGACY = new LegacyCommandNodeFactory();

  /**
   * Returns a literal node representing the given command.
   *
   * @param command the command
   * @param alias the case-insensitive command alias
   * @return the built literal node
   */
  LiteralCommandNode<CommandSource> create(final T command, final String alias);

  /**
   * Constructs the {@link LiteralCommandNode} representation of an {@link InvocableCommand}.
   *
   * @param <T> the type of the command
   * @param <I> the type of the command invocation
   * @param <A> the type of the command arguments
   */
  final class InvocableCommandNodeFactory
          <T extends InvocableCommand<I>, I extends CommandInvocation<A>, A>
          implements CommandNodeFactory<T> {

    private final CommandInvocationFactory<I> invocationFactory;
    private final ArgumentType<A> argumentsType;

    private InvocableCommandNodeFactory(final CommandInvocationFactory<I> invocationFactory,
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
              VelocityCommands.argumentBuilder(argumentsType, node)
                .suggests((context, builder) -> {
                  final I invocation = invocationFactory.create(context);
                  return command.suggestAsync(invocation).thenApply(suggestions -> {
                    for (String value : suggestions) {
                      Preconditions.checkNotNull(value, "suggestion");
                      builder.suggest(value);
                    }
                    return builder.build();
                  });
                })
                .build();
      node.addChild(argumentsNode);
      return node;
    }
  }

  /**
   * Constructs the {@link LiteralCommandNode} representation of a legacy command.
   */
  @Deprecated
  final class LegacyCommandNodeFactory implements CommandNodeFactory<Command> {

    @Override
    public LiteralCommandNode<CommandSource> create(final Command command, final String alias) {
      final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
              .<CommandSource>literal(alias.toLowerCase(Locale.ENGLISH))
              .requiresWithContext(parse -> {
                final String[] args = VelocityCommands.readArguments(
                        parse, String[].class, StringArrayArgumentType.EMPTY);
                return command.hasPermission(parse.getContext().getSource(), args);
              })
              .executes(context -> {
                final String[] args = VelocityCommands.readArguments(
                        context, String[].class, StringArrayArgumentType.EMPTY);
                command.execute(context.getSource(), args);
                return 1;
              })
              .build();

      final ArgumentCommandNode<CommandSource, String[]> argumentsNode =
              VelocityCommands.argumentBuilder(StringArrayArgumentType.INSTANCE, node)
                .suggests((context, builder) -> {
                  final String[] args = VelocityCommands.readArguments(
                          context, String[].class, StringArrayArgumentType.EMPTY);
                  return command.suggestAsync(context.getSource(), args).thenApply(suggestions -> {
                    for (String value : suggestions) {
                      Preconditions.checkNotNull(value, "suggestion");
                      builder.suggest(value);
                    }
                    return builder.build();
                  });
                })
                .build();
      node.addChild(argumentsNode);
      return node;
    }
  }
}
