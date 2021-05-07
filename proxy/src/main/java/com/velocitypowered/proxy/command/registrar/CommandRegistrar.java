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
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;

/**
 * Creates and registers the {@link LiteralCommandNode} representations of
 * a given {@link Command} in the root node of a {@link CommandDispatcher}.
 *
 * @param <T> the type of the command to register
 */
public interface CommandRegistrar<T extends Command> {

  /**
   * Registers the given command.
   *
   * @param command the command to register
   * @param meta the command metadata, including the case-insensitive aliases
   * @throws IllegalArgumentException if the given command cannot be registered
   */
  void register(final T command, final CommandMeta meta);

  /**
   * Returns the superclass or superinterface of all {@link Command} classes
   * compatible with this registrar. Note that {@link #register(Command, CommandMeta)}
   * may impose additional restrictions on individual {@link Command} instances.
   *
   * @return the superclass or superinterface of all the classes compatible with this registrar
   */
  Class<T> registrableSuperInterface();

  /**
   * Returns the dispatcher used for registration.
   *
   * @return the command dispatcher
   */
  CommandDispatcher<CommandSource> dispatcher();
}
