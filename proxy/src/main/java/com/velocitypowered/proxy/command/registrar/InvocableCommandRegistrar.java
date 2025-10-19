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

package com.velocitypowered.proxy.command.registrar;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.InvocableCommand;
import com.velocitypowered.proxy.command.VelocityCommandMeta;
import com.velocitypowered.proxy.command.VelocityCommands;
import com.velocitypowered.proxy.command.brigadier.VelocityArgumentBuilder;
import com.velocitypowered.proxy.command.brigadier.VelocityBrigadierCommandWrapper;
import com.velocitypowered.proxy.command.invocation.CommandInvocationFactory;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.function.Predicate;

/**
 * Base class for {@link CommandRegistrar}s capable of registering a subinterface of
 * {@link InvocableCommand} in a root node.
 */
abstract class InvocableCommandRegistrar<T extends InvocableCommand<I>,
    I extends CommandInvocation<A>, A> extends AbstractCommandRegistrar<T> {

  private final CommandInvocationFactory<I> invocationFactory;
  private final ArgumentType<A> argumentsType;

  protected InvocableCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock,
      final CommandInvocationFactory<I> invocationFactory,
      final ArgumentType<A> argumentsType) {
    super(root, lock);
    this.invocationFactory = Preconditions.checkNotNull(invocationFactory, "invocationFactory");
    this.argumentsType = Preconditions.checkNotNull(argumentsType, "argumentsType");
  }

  @Override
  public void register(final CommandMeta meta, final T command) {
    final Iterator<String> aliases = meta.getAliases().iterator();

    final String primaryAlias = aliases.next();
    final LiteralCommandNode<CommandSource> literal =
        this.createLiteral(command, meta, primaryAlias);
    this.register(literal);

    while (aliases.hasNext()) {
      final String alias = aliases.next();
      this.register(literal, alias);
    }
  }

  private LiteralCommandNode<CommandSource> createLiteral(final T command, final CommandMeta meta,
      final String alias) {
    final Predicate<CommandContextBuilder<CommandSource>> requirement = context -> {
      final I invocation = invocationFactory.create(context);
      return command.hasPermission(invocation);
    };
    final Command<CommandSource> callback = VelocityBrigadierCommandWrapper.wrap(context -> {
      final I invocation = invocationFactory.create(context);
      command.execute(invocation);
      return 1; // handled
    }, meta.getPlugin());

    final LiteralCommandNode<CommandSource> literal = LiteralArgumentBuilder
        .<CommandSource>literal(alias)
        .requiresWithContext((context, reader) -> {
          if (reader.canRead()) {
            // InvocableCommands do not follow a tree-like permissions checking structure.
            // Thus, a CommandSource may be able to execute a command with arguments while
            // not being able to execute the argument-less variant.
            // Only check for permissions once parsing is complete.
            return true;
          }
          return requirement.test(context);
        })
        .executes(callback)
        .build();

    final ArgumentCommandNode<CommandSource, String> arguments = VelocityArgumentBuilder
        .<CommandSource, A>velocityArgument(VelocityCommands.ARGS_NODE_NAME, argumentsType)
        .requiresWithContext((context, reader) -> requirement.test(context))
        .executes(callback)
        .suggests((context, builder) -> {
          // Offset the suggestion to the last space seperated word
          int lastSpace = builder.getRemaining().lastIndexOf(' ') + 1;
          final var offsetBuilder = builder.createOffset(builder.getStart() + lastSpace);

          final I invocation = invocationFactory.create(context);
          return command.suggestAsync(invocation).thenApply(suggestions -> {
            for (String value : suggestions) {
              Preconditions.checkNotNull(value, "suggestion");
              offsetBuilder.suggest(value);
            }
            return offsetBuilder.build();
          });
        })
        .build();
    literal.addChild(arguments);

    // Add hinting nodes
    VelocityCommandMeta.copyHints(meta).forEach(literal::addChild);

    return literal;
  }
}
