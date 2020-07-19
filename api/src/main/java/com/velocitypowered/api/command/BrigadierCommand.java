package com.velocitypowered.api.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;

/**
 * A command that uses Brigadier for parsing the command and providing
 * suggestions to the client.
 *
 * <p>Brigadier commands may be registered using a {@link Builder} instance,
 * obtainable by calling {@link CommandManager#brigadierBuilder()}.
 *
 * @see <a href="https://github.com/Mojang/brigadier#registering-a-new-command">How to create a command</a>
 */
public interface BrigadierCommand extends Command {

  /**
   * Provides a fluent interface to register a Brigadier command.
   */
  interface Builder extends Command.Builder<BrigadierCommand, Builder> {

    /**
     * Registers the command with the node returned by the specified builder.
     *
     * @param builder the {@link CommandNode} builder
     * @return the registered command
     */
    BrigadierCommand register(LiteralArgumentBuilder<CommandSource> builder);

    /**
     * Registers the command with the specified node.
     *
     * @param node the command node
     * @return the registered command
     */
    BrigadierCommand register(LiteralCommandNode<CommandSource> node);
  }
}
