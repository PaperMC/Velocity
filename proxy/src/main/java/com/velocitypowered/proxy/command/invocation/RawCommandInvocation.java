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

package com.velocitypowered.proxy.command.invocation;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.command.VelocityCommands;

public final class RawCommandInvocation extends AbstractCommandInvocation<String>
        implements RawCommand.Invocation {

  public static final Factory FACTORY = new Factory();

  private static final class Factory implements CommandInvocationFactory<RawCommand.Invocation> {

    @Override
    public RawCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String alias = VelocityCommands.readAlias(context);
      final String args = VelocityCommands.readArguments(context, String.class, "");
      return new RawCommandInvocation(context.getSource(), alias, args);
    }

    @Override
    public RawCommand.Invocation create(final CommandContextBuilder<CommandSource> context) {
      final String alias = VelocityCommands.readAlias(context);
      final String args = VelocityCommands.readArguments(context, String.class, "");
      return new RawCommandInvocation(context.getSource(), alias, args);
    }
  }

  private final String alias;

  private RawCommandInvocation(final CommandSource source,
                               final String alias, final String arguments) {
    super(source, arguments);
    this.alias = Preconditions.checkNotNull(alias, "alias");
  }

  @Override
  public String alias() {
    return this.alias;
  }

  @Override
  public String toString() {
    return "RawCommandInvocation{"
            + "source='" + this.source() + '\''
            + ", alias='" + this.alias + '\''
            + ", arguments='" + this.arguments() + '\''
            + '}';
  }
}
