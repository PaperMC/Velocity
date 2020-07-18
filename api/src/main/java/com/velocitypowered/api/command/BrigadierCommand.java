package com.velocitypowered.api.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

/**
 * A command that uses Brigadier for parsing the command and providing
 * suggestions to the client.
 *
 * <p>Brigadier commands may be registered using a {@link Builder} instance,
 * obtainable by calling {@link CommandManager#brigadierBuilder()}.
 *
 * @see <a href="https://github.com/Mojang/brigadier#registering-a-new-command">How to create a command</a>
 */
public interface BrigadierCommand extends Command<BrigadierCommandInvocation> {

  /**
   * Returns an {@link ArgumentBuilder} used to specify the structure of
   * a {@link CommandNode} executed by a {@link CommandSource}.
   *
   * <p>This wraps {@link LiteralArgumentBuilder#literal(String)}
   * explicitly setting the sender type, allowing it to be statically imported.
   * Thus, the following are equivalent:
   *
   * <pre> {@code
   * LiteralArgumentBuilder.<CommandSource>.literal(alias) // ...
   * argumentBuilder(alias) // ...
   * }</pre>
   *
   * @param alias the command alias
   * @return an argument builder
   * @see <a href="https://github.com/Mojang/brigadier/issues/35#issuecomment-429510335">issue</a>
   */
  static LiteralArgumentBuilder<CommandSource> argumentBuilder(final String alias) {
    return LiteralArgumentBuilder.literal(alias);
  }

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
    BrigadierCommand register(CommandNode<CommandSource> node);
  }
}
