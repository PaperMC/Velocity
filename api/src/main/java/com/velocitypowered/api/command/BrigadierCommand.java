package com.velocitypowered.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

/**
 * A command that uses Brigadier for parsing the command and providing
 * suggestions to the client.
 *
 * <p>Brigadier commands may be registered using a {@link Builder} instance
 * obtainable via the {@link CommandManager#brigadierBuilder()} method.
 */
public interface BrigadierCommand extends Command {

  /**
   * Return code used by a {@link com.mojang.brigadier.Command} to indicate
   * the command execution should be forwarded to the backend server.
   */
  int FORWARD = 0xF6287429;

  /**
   * Provides a fluent interface to register a Brigadier command.
   */
  interface Builder extends Command.Builder<BrigadierCommand, Builder> {

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
