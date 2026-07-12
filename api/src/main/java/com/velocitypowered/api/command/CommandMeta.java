/*
 * Copyright (C) 2020-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.command;

import com.mojang.brigadier.tree.CommandNode;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Contains metadata for a {@link Command}.
 */
public interface CommandMeta {

  /**
   * Returns a non-empty collection containing the case-insensitive aliases
   * used to execute the command.
   *
   * @return the command aliases
   */
  Collection<String> getAliases();

  /**
   * Returns an immutable collection containing command nodes that provide
   * additional argument metadata and tab-complete suggestions.
   * Note some {@link Command} implementations may not support hinting.
   *
   * @return the hinting command nodes
   */
  Collection<CommandNode<CommandSource>> getHints();

  /**
   * Returns the plugin who registered the command.
   * Note some {@link Command} registrations may not provide this information.
   *
   * @return the registering plugin
   */
  @Nullable Object getPlugin();

  /**
   * Returns whether partial invocations of this command are forwarded to the backend.
   *
   * @return whether to forward partial invocations
   */
  boolean forwardPartial();

  /**
   * Provides a fluent interface to create {@link CommandMeta}s.
   */
  interface Builder {

    /**
     * Specifies additional aliases that can be used to execute the command.
     *
     * @param aliases the command aliases
     * @return this builder, for chaining
     */
    Builder aliases(String... aliases);

    /**
     * Specifies a command node providing additional argument metadata and
     * tab-complete suggestions.
     *
     * @param node the command node
     * @return this builder, for chaining
     * @throws IllegalArgumentException if the node is executable, i.e. has a non-null
     *         {@link com.mojang.brigadier.Command}, or has a redirect.
     */
    Builder hint(CommandNode<CommandSource> node);

    /**
     * Specifies the plugin who registers the {@link Command}.
     *
     * @param plugin the registering plugin
     * @return this builder, for chaining
     */
    Builder plugin(Object plugin);

    /**
     * Specifies whether partial matches to this command are forwarded to the backend.
     *
     * <p>For example with the registered command "rootcommand -> subcommand" where only the subcommand is executable, this
     * specifies whether invocations such as "/rootcommand" or "/rootcommand nonexistant" should be forwarded to the
     * backend, or be handled on the proxy.
     *
     * @param fowardPartial whether to forward partial matches
     * @return this builder, for chaining
     */
    Builder forwardPartial(boolean fowardPartial);

    /**
     * Returns a newly-created {@link CommandMeta} based on the specified parameters.
     *
     * @return the built {@link CommandMeta}
     */
    CommandMeta build();
  }
}
