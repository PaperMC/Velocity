package com.velocitypowered.api.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

/**
 * A command that uses Brigadier for parsing the command and providing
 * suggestions to the client.
 */
public interface BrigadierCommand extends Command<BrigadierCommandInvocation> {

  /**
   * Returns an {@link ArgumentBuilder} used to specify the structure of
   * a {@link CommandNode} executed by a {@link CommandSource}.
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
