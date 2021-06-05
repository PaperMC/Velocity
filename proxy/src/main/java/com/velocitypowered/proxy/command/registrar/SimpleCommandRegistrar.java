package com.velocitypowered.proxy.command.registrar;

import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.proxy.command.brigadier.StringArrayArgumentType;
import com.velocitypowered.proxy.command.invocation.SimpleCommandInvocation;
import java.util.concurrent.locks.Lock;

/**
 * Registers {@link SimpleCommand}s in a root node.
 */
public final class SimpleCommandRegistrar
        extends InvocableCommandRegistrar<SimpleCommand, SimpleCommand.Invocation, String[]> {

  public SimpleCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock) {
    super(root, lock, SimpleCommandInvocation.FACTORY, StringArrayArgumentType.INSTANCE);
  }

  @Override
  public Class<SimpleCommand> registrableSuperInterface() {
    return SimpleCommand.class;
  }
}
