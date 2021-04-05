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

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Adds the permissible nodes to the given {@link RootCommandNode} for a {@link CommandSource}.
 *
 * @param <S> the type of the source to inject the nodes for
 */
public final class ClientCommandNodeInjector<S> {

  // TODO Test

  private static final StringRange FILTER_RANGE = StringRange.at(0);
  private static final StringReader FILTER_READER = new StringReader("");

  private final CommandDispatcher<S> dispatcher;

  ClientCommandNodeInjector(final CommandDispatcher<S> dispatcher) {
    this.dispatcher = dispatcher;
  }

  /**
   * Adds the nodes from the root node of this injector to the given root node,
   * respecting the requirements of the given source.
   *
   * <p>If the destination node previously contained a node with the same name
   * as one being inserted, the old node is removed from the node.
   *
   * <p>If the given node already contains a node with the same name, the node
   * is removed.
   *
   * @param source the command source to inject the nodes for
   * @param dest the root node to add the permissible nodes to
   */
  public void inject(final S source, final RootCommandNode<S> dest) {
    final CommandContextBuilder<S> rootContext =
            new CommandContextBuilder<>(dispatcher, source, getRoot(), 0);

    for (final CommandNode<S> alias : getRoot().getChildren()) {
      if (!alias.canUse(source)) {
        continue;
      }

      final CommandContextBuilder<S> context = rootContext.copy().withNode(alias, FILTER_RANGE);
      if (!alias.canUse(context, FILTER_READER)) {
        continue;
      }
      final ArgumentBuilder<S, ?> builder = alias.createBuilder();
      if (alias.getRedirect() != null) {
        CommandNode<S> target = alias.getRedirect();
        final CommandNode<S> targetArgsNode = target.getChild(VelocityCommands.ARGS_NODE_NAME);
        // Skip node filtering for non-BrigadierCommand alias redirects; if a source can
        // use an alias, then they can use all other aliases for the same Command.
        if (!VelocityCommands.isArgumentsNode(targetArgsNode)) {
          final CommandContextBuilder<S> childContext =
                  new CommandContextBuilder<>(dispatcher, source, context, target, 0);
          target = this.filterNode(target, childContext);
        }
        builder.redirect(target);
        this.addAlias(builder.build(), dest);
      } else {
        final CommandNode<S> argumentsNode = alias.getChild(VelocityCommands.ARGS_NODE_NAME);
        if (VelocityCommands.isArgumentsNode(argumentsNode)) {
          builder.then(argumentsNode);
          this.addAlias(builder.build(), dest);
        } else {
          // This is a BrigadierCommand, filter normally
          final CommandNode<S> node = builder.build();
          this.filterBrigadierCommand(alias, node, context);
          this.addAlias(node, dest);
        }
      }
    }
  }

  private void addAlias(final CommandNode<S> alias, final RootCommandNode<S> dest) {
    dest.removeChildByName(alias.getName()); // override commands from the backend server
    dest.addChild(alias);
  }

  private void filterBrigadierCommand(final CommandNode<S> alias,
                                                final CommandNode<S> dest,
                                                final CommandContextBuilder<S> contextSoFar) {
    final CommandContextBuilder<S> context = contextSoFar.copy().withNode(alias, FILTER_RANGE);
    this.filterChildren(alias, dest, context);
  }

  private @Nullable CommandNode<S> filterNode(final CommandNode<S> node,
                                              final CommandContextBuilder<S> contextSoFar) {
    final S source = contextSoFar.getSource();
    if (!node.canUse(source)) {
      return null;
    }
    final CommandContextBuilder<S> context = contextSoFar.copy().withNode(node, FILTER_RANGE);
    if (!node.canUse(context, FILTER_READER)) {
      return null;
    }
    final ArgumentBuilder<S, ?> builder = node.createBuilder();
    if (node.getRedirect() != null) {
      final CommandContextBuilder<S> childContext =
              new CommandContextBuilder<>(dispatcher, source, context, node.getRedirect(), 0);
      builder.redirect(this.filterNode(node.getRedirect(), childContext));
    }

    final CommandNode<S> result = builder.build();
    this.filterChildren(node, result, context);
    return result;
  }

  private void filterChildren(final CommandNode<S> source, final CommandNode<S> dest,
                              final CommandContextBuilder<S> context) {
    for (final CommandNode<S> child : source.getChildren()) {
      CommandNode<S> filtered = this.filterNode(child, context);
      if (filtered != null) {
        dest.addChild(filtered);
      }
    }
  }

  private RootCommandNode<S> getRoot() {
    return dispatcher.getRoot();
  }
}
