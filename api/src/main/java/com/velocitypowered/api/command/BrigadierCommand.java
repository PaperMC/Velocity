package com.velocitypowered.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.function.Predicate;

/**
 * A command that uses Brigadier for parsing the command and providing
 * suggestions to the client.
 *
 * <p>Brigadier commands may be registered using a {@link Builder} instance
 * obtainable via the {@link CommandManager#brigadierBuilder()} method.
 */
public interface BrigadierCommand extends Command {

  /**
   * Provides a fluent interface to register a Brigadier command.
   */
  interface Builder extends Command.Builder<BrigadierCommand, Builder> {

    /**
     * Specifies the permission-checking predicate the {@link CommandSource}
     * must pass to execute the command.
     *
     * <p>Brigadier excludes nodes that don't pass the {@link CommandNode#getRequirement()}
     * predicate from parse results. Implementations therefore cannot
     * differentiate between a missing command alias or a {@code false} return
     * from the predicate, leading to command executions being redirected to
     * the backend server. This method instead wraps all the commands of
     * the registered node to prevent this redirect.
     *
     * @param predicate the permission-checking predicate
     * @return this builder, for chaining
     */
    Builder permission(Predicate<CommandContext<CommandSource>> predicate);

    /**
     * Registers the command with the node returned by the specified builder.
     *
     * @param builder the {@link CommandNode} builder
     * @return the registered command
     * @throws IllegalArgumentException if one of the given aliases is already registered
     */
    BrigadierCommand register(LiteralArgumentBuilder<CommandSource> builder);

    /**
     * Registers the command with the specified node.
     *
     * @param node the command node
     * @return the registered command
     * @throws IllegalArgumentException if one of the given aliases is already registered
     */
    BrigadierCommand register(LiteralCommandNode<CommandSource> node);
  }
}
