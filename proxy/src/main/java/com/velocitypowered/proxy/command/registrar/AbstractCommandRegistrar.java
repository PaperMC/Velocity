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

import com.google.common.base.Preconditions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandSource;

/**
 * Base class for {@link CommandRegistrar} implementations.
 *
 * @param <T> the type of the command to register
 */
abstract class AbstractCommandRegistrar<T extends Command> implements CommandRegistrar<T> {

  private final CommandDispatcher<CommandSource> dispatcher;

  protected AbstractCommandRegistrar(final CommandDispatcher<CommandSource> dispatcher) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
  }

  protected void register(final LiteralCommandNode<CommandSource> node) {
    final RootCommandNode<CommandSource> root = this.dispatcher.getRoot();

    // Registration overrides previous aliased command
    root.removeChildByName(node.getName());
    root.addChild(node);
  }

  protected void register(final LiteralCommandNode<CommandSource> node,
                          final String secondaryAlias) {
    final LiteralCommandNode<CommandSource> copy = this.createAlias(node, secondaryAlias);
    this.register(copy);
  }

  /**
   * Creates a copy of the given literal with the specified name.
   *
   * @param from the literal node to copy
   * @param secondaryAlias the name of the returned literal node
   * @return a copy of the literal with the given name
   */
  protected LiteralCommandNode<CommandSource> createAlias(
          final LiteralCommandNode<CommandSource> from, final String secondaryAlias) {
    // Brigadier resolves the redirect of a node if further input can be parsed.
    // Let <bar> be a literal node having a redirect to a <foo> literal. Then,
    // the context returned by CommandDispatcher#parseNodes when given the input
    // string "<bar> " does not contain a child context with <foo> as its root node.
    // Thus, the vanilla client asks the children of <bar> for suggestions, instead
    // of those of <foo> (https://github.com/Mojang/brigadier/issues/46).
    // Perform a shallow copy of the literal instead.
    Preconditions.checkNotNull(from, "from");
    Preconditions.checkNotNull(secondaryAlias, "secondaryAlias");
    final LiteralArgumentBuilder<CommandSource> builder = LiteralArgumentBuilder
            .<CommandSource>literal(secondaryAlias)
            .requires(from.getRequirement())
            .requiresWithContext(from.getContextRequirement())
            .forward(from.getRedirect(), from.getRedirectModifier(), from.isFork())
            .executes(from.getCommand());
    for (final CommandNode<CommandSource> child : from.getChildren()) {
      builder.then(child);
    }
    return builder.build();
  }

  @Override
  public CommandDispatcher<CommandSource> dispatcher() {
    return this.dispatcher;
  }
}
