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

package com.velocitypowered.proxy.command.registrar;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.command.VelocityCommands;
import com.velocitypowered.proxy.command.brigadier.GreedyArgumentBuilder;
import com.velocitypowered.proxy.command.brigadier.StringArrayArgumentType;
import java.util.Iterator;
import java.util.function.Predicate;

/**
 * Registers legacy commands in the root node of a {@link CommandDispatcher}.
 */
public final class LegacyCommandRegistrar extends AbstractCommandRegistrar<Command> {

  public LegacyCommandRegistrar(final CommandDispatcher<CommandSource> dispatcher) {
    super(dispatcher);
  }

  @Override
  public void register(final Command command, final CommandMeta meta) {
    final Iterator<String> aliases = meta.getAliases().iterator();

    final String primaryAlias = aliases.next();
    final LiteralCommandNode<CommandSource> literal = this.createLiteral(command, primaryAlias);
    this.register(literal);

    while (aliases.hasNext()) {
      final String alias = aliases.next();
      this.register(literal, alias);
    }
  }

  private LiteralCommandNode<CommandSource> createLiteral(final Command command,
                                                          final String alias) {
    final Predicate<CommandContextBuilder<CommandSource>> requirement = context -> {
      final String[] arguments = VelocityCommands
              .readArguments(context, String[].class, StringArrayArgumentType.EMPTY);
      return command.hasPermission(context.getSource(), arguments);
    };
    final com.mojang.brigadier.Command<CommandSource> callback = context -> {
      final String[] arguments = VelocityCommands
              .readArguments(context, String[].class, StringArrayArgumentType.EMPTY);
      command.execute(context.getSource(), arguments);
      return 1; // handled
    };

    final LiteralCommandNode<CommandSource> literal = LiteralArgumentBuilder
            .<CommandSource>literal(alias)
            .requiresWithContext((context, reader) -> {
              if (reader.canRead()) {
                // See the comment on InvocableCommandRegistrar#createLiteral about
                // the non-tree-like permission checking structure.
                return true;
              }
              return requirement.test(context);
            })
            .executes(callback)
            .build();

    final ArgumentCommandNode<CommandSource, String> arguments = GreedyArgumentBuilder
            .<CommandSource, String[]>greedyArgument(
                    VelocityCommands.ARGS_NODE_NAME, StringArrayArgumentType.INSTANCE)
            .requiresWithContext((context, reader) -> requirement.test(context))
            .executes(callback)
            .suggests((context, builder) -> {
              final String[] args = VelocityCommands
                      .readArguments(context, String[].class, StringArrayArgumentType.EMPTY);
              return command.suggestAsync(context.getSource(), args).thenApply(suggestions -> {
                for (String value : suggestions) {
                  Preconditions.checkNotNull(value, "suggestion");
                  builder.suggest(value);
                }
                return builder.build();
              });
            })
            .build();
    literal.addChild(arguments);
    // Legacy commands don't support hinting
    return literal;
  }

  @Override
  public Class<Command> registrableSuperInterface() {
    return Command.class;
  }
}
