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
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.util.BrigadierUtils;

final class VelocityRawCommandInvocation extends AbstractCommandInvocation<String>
        implements RawCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<RawCommand.Invocation> {

    @Override
    public RawCommand.Invocation create(final CommandContext<CommandSource> context) {
      return new VelocityRawCommandInvocation(
              context.getSource(),
              BrigadierUtils.getAlias(context),
              BrigadierUtils.getRawArguments(context));
    }
  }

  private final String alias;

  private VelocityRawCommandInvocation(final CommandSource source,
                               final String alias, final String arguments) {
    super(source, arguments);
    this.alias = Preconditions.checkNotNull(alias);
  }

  @Override
  public String alias() {
    return alias;
  }
}
