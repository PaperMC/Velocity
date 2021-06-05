package com.velocitypowered.proxy.command.registrar;

import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import com.velocitypowered.api.command.Command;
import com.velocitypowered.api.command.CommandMeta;

/**
 * Creates and registers the {@link LiteralCommandNode} representations of
 * a given {@link Command} in a {@link RootCommandNode}.
 *
 * @param <T> the type of the command to register
 */
public interface CommandRegistrar<T extends Command> {

  /**
   * Registers the given command.
   *
   * @param command the command to register
   * @param meta the command metadata, including the case-insensitive aliases
   * @throws IllegalArgumentException if the given command cannot be registered
   */
  void register(final T command, final CommandMeta meta);

  /**
   * Returns the superclass or superinterface of all {@link Command} classes
   * compatible with this registrar. Note that {@link #register(Command, CommandMeta)}
   * may impose additional restrictions on individual {@link Command} instances.
   *
   * @return the superclass of all the classes compatible with this registrar
   */
  Class<T> registrableSuperInterface();
}
