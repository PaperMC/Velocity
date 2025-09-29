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

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;

/**
 * Creates and registers the {@link LiteralCommandNode} representations of a given {@link Command}
 * in a {@link RootCommandNode}.
 *
 * @param <T> the type of the command to register
 */
public interface CommandRegistrar<T extends Command> {

  /**
   * Registers the given command.
   *
   * @param meta    the command metadata, including the case-insensitive aliases
   * @param command the command to register
   * @throws IllegalArgumentException if the given command cannot be registered
   */
  void register(final CommandMeta meta, final T command);

  /**
   * Returns the superclass or superinterface of all {@link Command} classes compatible with this
   * registrar. Note that {@link #register(CommandMeta, Command)} may impose additional restrictions
   * on individual {@link Command} instances.
   *
   * @return the superclass of all the classes compatible with this registrar
   */
  Class<T> registrableSuperInterface();
}
