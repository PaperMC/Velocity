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
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import java.util.Locale;

/**
 * Registers {@link BrigadierCommand}s in the root node of a {@link CommandDispatcher}.
 */
public final class BrigadierCommandRegistrar extends AbstractCommandRegistrar<BrigadierCommand> {

  public BrigadierCommandRegistrar(final CommandDispatcher<CommandSource> dispatcher) {
    super(dispatcher);
  }

  @Override
  public void register(final BrigadierCommand command, final CommandMeta meta) {
    // The literal name might not match any aliases on the given meta.
    // Register it (if valid), since it's probably what the user expects.
    final LiteralCommandNode<CommandSource> literal = command.getNode();
    final String primaryAlias = literal.getName();
    if (this.isValidAlias(primaryAlias)) {
      // Register directly without copying
      this.register(literal);
    }

    for (final String alias : meta.getAliases()) {
      if (primaryAlias.equals(alias)) {
        continue;
      }
      this.register(literal, alias);
    }

    // BrigadierCommands don't support hinting
    // TODO(velocity-2): throw if meta contains hint nodes
  }

  private boolean isValidAlias(final String alias) {
    return alias.equals(alias.toLowerCase(Locale.ENGLISH));
  }

  @Override
  public Class<BrigadierCommand> registrableSuperInterface() {
    return BrigadierCommand.class;
  }
}
