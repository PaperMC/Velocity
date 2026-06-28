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
import com.velocitypowered.proxy.command.brigadier.VelocityArgumentCommandNode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import org.checkerframework.checker.lock.qual.GuardedBy;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Copies the nodes of a {@link RootCommandNode} to a possibly non-empty destination
 * {@link RootCommandNode}, respecting the requirements satisfied by a given command source.
 *
 * @param <S> the type of the source to inject the nodes for
 */
public final class CommandGraphInjector<S> {

  private static final StringRange ALIAS_RANGE = StringRange.at(0);
  private static final StringReader ALIAS_READER = new StringReader("");

  private final @GuardedBy("lock") CommandDispatcher<S> dispatcher;
  private final Lock lock;

  CommandGraphInjector(final CommandDispatcher<S> dispatcher, final Lock lock) {
    this.dispatcher = Preconditions.checkNotNull(dispatcher, "dispatcher");
    this.lock = Preconditions.checkNotNull(lock, "lock");
  }

  // The term "source" is ambiguous here. We use "origin" when referring to
  // the root node we are copying nodes from to the destination node.

  /**
   * A read-only snapshot of a {@link CommandNode} taken under the injector's lock, capturing the
   * node reference plus snapshots of its children and redirect target. Brigadier's
   * {@code getChildren()} returns a live view over a non-thread-safe {@code LinkedHashMap}, so
   * iterating it outside the lock risks a {@code ConcurrentModificationException}. Snapshots let
   * the permission checks and node copying (which run plugin code) proceed without holding the
   * lock, so a slow {@code canUse} predicate on one player's switch no longer blocks command
   * parsing/suggestions for every other player.
   */
  private static final class Snapshot<S> {

    final CommandNode<S> node;
    final List<Snapshot<S>> children = new ArrayList<>();
    @Nullable
    Snapshot<S> redirect;

    Snapshot(final CommandNode<S> node) {
      this.node = node;
    }
  }

  /**
   * Adds the node from the root node of this injector to the given root node, respecting the
   * requirements satisfied by the given source.
   *
   * <p>Prior to adding a literal with the same name as one previously contained
   * in the destination node, the old node is removed from the destination node.
   *
   * @param dest   the root node to add the permissible nodes to
   * @param source the command source to inject the nodes for
   */
  public void inject(final RootCommandNode<S> dest, final S source) {
    final RootCommandNode<S> origin = this.dispatcher.getRoot();

    // bVelocity: snapshot the reachable command tree under the lock, then release it. The
    // permission checks (canUse) and node copying below may run arbitrary plugin code that can be
    // slow; doing that work under the read lock serializes every player's command-graph injection
    // (and, via the shared lock, Tab completion and command parsing). Snapshots are consistent at
    // capture time; a concurrent command registration may let a player see a slightly stale tree,
    // which is acceptable.
    final List<Snapshot<S>> rootChildren;
    final Map<CommandNode<S>, Snapshot<S>> snapshots = new IdentityHashMap<>();
    lock.lock();
    try {
      rootChildren = new ArrayList<>();
      for (final CommandNode<S> child : origin.getChildren()) {
        rootChildren.add(this.snapshot(child, snapshots));
      }
    } finally {
      lock.unlock();
    }

    final Map<CommandNode<S>, CommandNode<S>> done = new IdentityHashMap<>();
    final CommandContextBuilder<S> rootContext =
        new CommandContextBuilder<>(this.dispatcher, source, origin, 0);

    // Filter alias nodes
    for (final Snapshot<S> snap : rootChildren) {
      final CommandNode<S> node = snap.node;
      if (!node.canUse(source)) {
        continue;
      }

      final CommandContextBuilder<S> context = rootContext.copy()
          .withNode(node, ALIAS_RANGE);
      if (!node.canUse(context, ALIAS_READER)) {
        continue;
      }

      final LiteralCommandNode<S> asLiteral = (LiteralCommandNode<S>) node;
      final LiteralCommandNode<S> copy = asLiteral.createBuilder().build();
      final VelocityArgumentCommandNode<S, ?> argsNode = findArgumentsNode(snap);
      if (argsNode == null) {
        // This literal is associated to a BrigadierCommand, filter normally.
        this.copyChildren(snap, copy, source, done);
      } else {
        // Copy all children nodes (arguments node and hints)
        for (final Snapshot<S> child : snap.children) {
          copy.addChild(child.node);
        }
      }
      this.addAlias(copy, dest);
    }
  }

  /**
   * Recursively captures a snapshot of the given node (and its reachable descendants / redirect
   * target) into {@code visited}, deduplicating by node identity to handle redirects and cycles.
   * Must be called under the lock.
   */
  private Snapshot<S> snapshot(final CommandNode<S> node,
      final Map<CommandNode<S>, Snapshot<S>> visited) {
    Snapshot<S> snap = visited.get(node);
    if (snap != null) {
      return snap;
    }
    snap = new Snapshot<>(node);
    visited.put(node, snap);
    for (final CommandNode<S> child : node.getChildren()) {
      snap.children.add(this.snapshot(child, visited));
    }
    final CommandNode<S> redirect = node.getRedirect();
    if (redirect != null) {
      snap.redirect = this.snapshot(redirect, visited);
    }
    return snap;
  }

  private @Nullable CommandNode<S> filterNode(final Snapshot<S> snap, final S source,
      final Map<CommandNode<S>, CommandNode<S>> done) {
    final CommandNode<S> node = snap.node;
    final CommandNode<S> existing = done.get(node);
    if (existing != null) {
      return existing;
    }
    // We only check the non-context requirement when filtering alias nodes.
    // Otherwise, we would need to manually craft context builder and reader instances,
    // which is both incorrect and inefficient. The reason why we can do so for alias
    // literals is due to the empty string being a valid and expected input by
    // the context-aware requirement (when suggesting the literal name).
    if (!node.canUse(source)) {
      return null;
    }
    final ArgumentBuilder<S, ?> builder = node.createBuilder();
    if (snap.redirect != null) {
      // Redirects to non-Brigadier commands are not supported. Luckily,
      // we don't expose the root node to API users, so they can't access
      // nodes associated to other commands.
      final CommandNode<S> target = this.filterNode(snap.redirect, source, done);
      builder.forward(target, builder.getRedirectModifier(), builder.isFork());
    }
    final CommandNode<S> result = builder.build();
    done.put(node, result);
    for (final Snapshot<S> child : snap.children) {
      final CommandNode<S> filtered = this.filterNode(child, source, done);
      if (filtered != null) {
        result.addChild(filtered);
      }
    }
    return result;
  }

  private void copyChildren(final Snapshot<S> parent, final CommandNode<S> dest, final S source,
      final Map<CommandNode<S>, CommandNode<S>> done) {
    for (final Snapshot<S> child : parent.children) {
      final CommandNode<S> filtered = this.filterNode(child, source, done);
      if (filtered != null) {
        dest.addChild(filtered);
      }
    }
  }

  /**
   * Finds the arguments node among a snapshot's children without touching the live command tree.
   *
   * @param snap the alias snapshot
   * @param <S>  the source type
   * @return the arguments node, or {@code null} if not present
   */
  private static <S> @Nullable VelocityArgumentCommandNode<S, ?> findArgumentsNode(
      final Snapshot<S> snap) {
    for (final Snapshot<S> child : snap.children) {
      if (VelocityCommands.isArgumentsNode(child.node)) {
        return (VelocityArgumentCommandNode<S, ?>) child.node;
      }
    }
    return null;
  }

  private void addAlias(final LiteralCommandNode<S> node, final RootCommandNode<S> dest) {
    dest.removeChildByName(node.getName());
    dest.addChild(node);
  }
}
