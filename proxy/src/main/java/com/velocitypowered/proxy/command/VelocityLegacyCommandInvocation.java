package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;

final class VelocityLegacyCommandInvocation extends AbstractCommandInvocation<String[]>
        implements LegacyCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static String[] split(final String line) {
    if (line.isEmpty()) {
      return new String[0];
    }
    return line.trim().split(" ", -1);
  }

  static class Factory implements CommandInvocationFactory<LegacyCommand.Invocation> {

    @Override
    public LegacyCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String[] arguments = split(getArguments(context));
      return new VelocityLegacyCommandInvocation(context.getSource(), arguments);
    }
  }

  VelocityLegacyCommandInvocation(final CommandSource source, final String[] arguments) {
    super(source, arguments);
  }
}
