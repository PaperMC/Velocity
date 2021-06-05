package com.velocitypowered.proxy.command.registrar;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.command.invocation.RawCommandInvocation;
import java.util.concurrent.locks.Lock;

/**
 * Registers {@link RawCommand}s in a root node.
 */
public final class RawCommandRegistrar
        extends InvocableCommandRegistrar<RawCommand, RawCommand.Invocation, String> {

  public RawCommandRegistrar(final RootCommandNode<CommandSource> root, final Lock lock) {
    super(root, lock, RawCommandInvocation.FACTORY, StringArgumentType.greedyString());
  }

  @Override
  public Class<RawCommand> registrableSuperInterface() {
    return RawCommand.class;
  }
}
