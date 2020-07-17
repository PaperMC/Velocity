package com.velocitypowered.proxy.command;

import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

@FunctionalInterface
public interface CommandExecutionContextFactory<C extends CommandInvocation> {

  // TODO Document
  C createContext(final CommandSource source, final String commandLine);

  // TODO Document
  default boolean argsCommandLine() {
    return true;
  }

  CommandExecutionContextFactory<CommandInvocation> FALLBACK = new FallbackCommandExecutionContextFactory();

  final class FallbackCommandExecutionContextFactory
          implements CommandExecutionContextFactory<CommandInvocation> {

    @Override
    public CommandInvocation createContext(final CommandSource source, final String commandLine) {
      return () -> source;
    }
  }
}
