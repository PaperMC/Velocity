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

import com.mojang.brigadier.CommandDispatcher;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.command.brigadier.StringArrayArgumentType;
import com.velocitypowered.proxy.command.invocation.SimpleCommandInvocation;

/**
 * Registers {@link SimpleCommand}s in the root node of a {@link CommandDispatcher}.
 */
public final class SimpleCommandRegistrar
        extends InvocableCommandRegistrar<SimpleCommand, SimpleCommand.Invocation, String[]> {

  public SimpleCommandRegistrar(final CommandDispatcher<CommandSource> dispatcher) {
    super(dispatcher, SimpleCommandInvocation.FACTORY, StringArrayArgumentType.INSTANCE);
  }

  @Override
  public Class<SimpleCommand> registrableSuperInterface() {
    return SimpleCommand.class;
  }
}
