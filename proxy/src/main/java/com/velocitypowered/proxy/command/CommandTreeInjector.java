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
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.proxy.command.brigadier.GreedyArgumentCommandNode;
import jdk.internal.jline.internal.Nullable;

/**
 * Copies the nodes of a {@link RootCommandNode} to a possibly non-empty
 * destination {@link RootCommandNode}, respecting the requirements satisfied
 * by a given command source.
 *
 * @param <S> the type of the source to inject the nodes for
 */
public final class CommandTreeInjector<S> {

  private static final StringRange ALIAS_RANGE = StringRange.at(0);
  private static final StringReader ALIAS_READER = new StringReader("");

  private final CommandDispatcher<S> dispatcher;

  CommandTreeInjector(final CommandDispatcher<S> dispatcher) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
  }

  /**
   * Adds the nodes from the root node of this injector to the given root node,
   * respecting the requirements satisfied by the given source.
   *
   * <p>Prior to adding a literal with the same name as one previously contained
   * in the destination tree, the old node is removed from the destination node.
   *
   * @param source the command source to inject the nodes for
   * @param dest the root node to add the permissible nodes to
   */
  public void inject(final S source, final RootCommandNode<S> dest) {
    final CommandContextBuilder<S> rootContext =
            new CommandContextBuilder<>(this.dispatcher, source, this.root(), 0);

    for (final CommandNode<S> node : this.root().getChildren()) {
      if (!node.canUse(source)) {
        continue;
      }

      final CommandContextBuilder<S> context = rootContext.copy();
      context.withNode(node, ALIAS_RANGE);
      if (!node.canUse(context, ALIAS_READER)) {
        continue;
      }

      final LiteralCommandNode<S> asLiteral = (LiteralCommandNode<S>) node;
      final LiteralCommandNode<S> copy = asLiteral.createBuilder().build();

      final GreedyArgumentCommandNode<S, ?> argsNode = VelocityCommands.getArgumentsNode(asLiteral);
      if (argsNode == null) {
        // This literal comes from a BrigadierCommand, filter normally
        this.filterChildren(node, copy, source);
      } else {
        // Copy all children nodes (arguments node and hints)
        for (CommandNode<S> child : node.getChildren()) {
          copy.addChild(child);
        }
      }
      this.addAlias(copy, dest);
    }
  }

  private @Nullable CommandNode<S> filterNode(final CommandNode<S> node, final S source) {
    // We only check the non-context requirement when filtering nodes.
    // Otherwise, we would need to craft context builder and reader instances which
    // is both incorrect and inefficient. The reason why we can do so for alias literals
    // is due to the empty string being a valid and expected input by the context-aware
    // requirement (when suggesting the literal name).
    if (!node.canUse(source)) {
      return null;
    }
    final ArgumentBuilder<S, ?> builder = node.createBuilder();
    if (node.getRedirect() != null) {
      // TODO Document redirects to non-Brigadier commands are not supported. Or test instead?
      final CommandNode<S> target = this.filterNode(node.getRedirect(), source);
      builder.forward(target, builder.getRedirectModifier(), builder.isFork());
    }
    final CommandNode<S> result = builder.build();
    this.filterChildren(node, result, source);
    return result;
  }

  private void filterChildren(final CommandNode<S> parent, final CommandNode<S> dest,
                              final S source) {
    for (final CommandNode<S> child : parent.getChildren()) {
      final CommandNode<S> filtered = this.filterNode(child, source);
      if (filtered != null) {
        dest.addChild(filtered);
      }
    }
  }

  private void addAlias(final LiteralCommandNode<S> node, final RootCommandNode<S> dest) {
    dest.removeChildByName(node.getName());
    dest.addChild(node);
  }

  private RootCommandNode<S> root() {
    return this.dispatcher.getRoot();
  }
}
