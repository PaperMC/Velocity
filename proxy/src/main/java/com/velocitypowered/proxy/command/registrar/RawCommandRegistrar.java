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

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.command.invocation.RawCommandInvocation;
import java.util.concurrent.locks.Lock;

/**
 * Registers {@link RawCommand}s in a root node.
 */
public final class RawCommandRegistrar
    extends InvocableCommandRegistrar<RawCommand, RawCommand.Invocation, String> {

  public RawCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock) {
    super(root, lock, RawCommandInvocation.FACTORY, StringArgumentType.greedyString());
  }

  @Override
  public Class<RawCommand> registrableSuperInterface() {
    return RawCommand.class;
  }
}
