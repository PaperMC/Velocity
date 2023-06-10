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

import com.google.common.base.Preconditions;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.command.VelocityCommands;
import java.util.concurrent.locks.Lock;
import org.checkerframework.checker.lock.qual.GuardedBy;

/**
 * Base class for {@link CommandRegistrar} implementations.
 *
 * @param <T> the type of the command to register
 */
abstract class AbstractCommandRegistrar<T extends Command> implements CommandRegistrar<T> {

  private final @GuardedBy("lock") RootCommandNode<CommandSource> root;
  private final Lock lock;

  protected AbstractCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock) {
    this.root = Preconditions.checkNotNull(root, "root");
    this.lock = Preconditions.checkNotNull(lock, "lock");
  }

  protected void register(final LiteralCommandNode<CommandSource> node) {
    lock.lock();
    try {
      // Registration overrides previous aliased command
      root.removeChildByName(node.getName());
      root.addChild(node);
    } finally {
      lock.unlock();
    }
  }

  protected void register(final LiteralCommandNode<CommandSource> node,
      final String secondaryAlias) {
    final LiteralCommandNode<CommandSource> copy =
        VelocityCommands.shallowCopy(node, secondaryAlias);
    this.register(copy);
  }
}
