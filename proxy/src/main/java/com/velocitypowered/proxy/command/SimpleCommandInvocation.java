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
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

final class SimpleCommandInvocation extends AbstractCommandInvocation<String[]>
        implements SimpleCommand.Invocation {

  static final Factory FACTORY = new Factory();

  private static final class Factory implements CommandInvocationFactory<SimpleCommand.Invocation> {

    @Override
    public SimpleCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String alias = VelocityCommands.readAlias(context);
      final String[] args = VelocityCommands.readArguments(
              context, String[].class, StringArrayArgumentType.EMPTY);
      return new SimpleCommandInvocation(context.getSource(), alias, args);
    }

    @Override
    public SimpleCommand.Invocation create(final CommandContextBuilder<CommandSource> context) {
      final String alias = VelocityCommands.readAlias(context);
      final String[] args = VelocityCommands
              .readArguments(context, String[].class, StringArrayArgumentType.EMPTY);
      return new SimpleCommandInvocation(context.getSource(), alias, args);
    }
  }

  private final String alias;

  private SimpleCommandInvocation(final CommandSource source, final String alias,
                                  final String[] arguments) {
    super(source, arguments);
    this.alias = Preconditions.checkNotNull(alias, "alias");
  }

  @Override
  public String alias() {
    return this.alias;
  }

  @Override
  public String toString() {
    return "SimpleCommandInvocation{"
            + "alias='" + this.alias + '\''
            + '}';
  }
}
