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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.command.invocation.RawCommandInvocation;

/**
 * Registers {@link RawCommand}s in the root node of a {@link CommandDispatcher}.
 */
public final class RawCommandRegistrar
        extends InvocableCommandRegistrar<RawCommand, RawCommand.Invocation, String> {

  public RawCommandRegistrar(final CommandDispatcher<CommandSource> dispatcher) {
    super(dispatcher, RawCommandInvocation.FACTORY, StringArgumentType.greedyString());
  }

  @Override
  public void register(final RawCommand command, final CommandMeta meta) {
    // We need to detect if this is a "legacy" RawCommand implementation.
    // If so, throw IAE and let another registrar create and register the literal.
    // This ugly hack will be removed in Velocity 2.0. Most if not all plugins
    // have side-effect free #suggest methods. We rely on the newer RawCommand
    // throwing UOE.
    try {
      command.suggest(null, new String[0]);
    } catch (final UnsupportedOperationException ignored) {
      // This is probably a RawCommand using the method variants taking Invocation objects.
      super.register(command, meta);
    } catch (final Exception e) {
      throw new IllegalArgumentException("Cannot register legacy raw command", e);
    }
  }

  @Override
  public Class<RawCommand> registrableSuperInterface() {
    return RawCommand.class;
  }
}
