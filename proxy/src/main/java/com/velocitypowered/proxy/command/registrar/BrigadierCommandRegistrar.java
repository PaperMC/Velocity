/*
 * Copyright (C) 2021 Velocity Contributors
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

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.command.VelocityCommands;
import java.util.concurrent.locks.Lock;

/**
 * Registers {@link BrigadierCommand}s in a root node.
 */
public final class BrigadierCommandRegistrar extends AbstractCommandRegistrar<BrigadierCommand> {

  public BrigadierCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock) {
    super(root, lock);
  }

  @Override
  public void register(final CommandMeta meta, final BrigadierCommand command) {
    // The literal name might not match any aliases on the given meta.
    // Register it (if valid), since it's probably what the user expects.
    // If invalid, the metadata contains the same alias, but in lowercase.
    final LiteralCommandNode<CommandSource> literal = command.getNode();
    final String primaryAlias = literal.getName();
    if (VelocityCommands.isValidAlias(primaryAlias)) {
      // Register directly without copying
      this.register(literal);
    }

    for (final String alias : meta.getAliases()) {
      if (primaryAlias.equals(alias)) {
        continue;
      }
      this.register(literal, alias);
    }

    // Brigadier commands don't support hinting, ignore
  }

  @Override
  public Class<BrigadierCommand> registrableSuperInterface() {
    return BrigadierCommand.class;
  }
}
