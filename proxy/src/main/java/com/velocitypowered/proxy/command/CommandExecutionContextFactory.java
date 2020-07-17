package com.velocitypowered.proxy.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandExecutionContext;
import com.velocitypowered.api.command.CommandSource;

@FunctionalInterface
public interface CommandExecutionContextFactory<C extends CommandExecutionContext> {

  // TODO Document
  C createContext(final CommandSource source, final String commandLine) throws CommandSyntaxException;

  // TODO Document
  default boolean argsCommandLine() {
    return true;
  }

  CommandExecutionContextFactory<CommandExecutionContext> FALLBACK = new FallbackCommandExecutionContextFactory();

  final class FallbackCommandExecutionContextFactory
          implements CommandExecutionContextFactory<CommandExecutionContext> {

    @Override
    public CommandExecutionContext createContext(final CommandSource source, final String commandLine) {
      return () -> source;
    }
  }
}
