package com.velocitypowered.proxy.command.registrar;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.command.VelocityCommands;
import java.util.concurrent.locks.Lock;

/**
 * Registers {@link BrigadierCommand}s in a root node.
 */
public final class BrigadierCommandRegistrar extends AbstractCommandRegistrar<BrigadierCommand> {

  public BrigadierCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock) {
    super(root, lock);
  }

  @Override
  public void register(final BrigadierCommand command, final CommandMeta meta) {
    // The literal name might not match any aliases on the given meta.
    // Register it (if valid), since it's probably what the user expects.
    // If invalid, the metadata contains the same alias, but in lowercase.
    final LiteralCommandNode<CommandSource> literal = command.getNode();
    final String primaryAlias = literal.getName();
    if (VelocityCommands.isValidAlias(primaryAlias)) {
      // Register directly without copying
      this.register(literal);
    }

    for (final String alias : meta.getAliases()) {
      if (primaryAlias.equals(alias)) {
        continue;
      }
      this.register(literal, alias);
    }

    // Brigadier commands don't support hinting, ignore
  }

  @Override
  public Class<BrigadierCommand> registrableSuperInterface() {
    return BrigadierCommand.class;
  }
}
