package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;
import com.velocitypowered.proxy.util.BrigadierUtils;

final class VelocityLegacyCommandInvocation extends AbstractCommandInvocation<String[]>
        implements LegacyCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<LegacyCommand.Invocation> {

    @Override
    public LegacyCommand.Invocation create(final CommandContext<CommandSource> context) {
      final String[] arguments = BrigadierUtils.getSplitArguments(context);
      return new VelocityLegacyCommandInvocation(context.getSource(), arguments);
    }
  }

  VelocityLegacyCommandInvocation(final CommandSource source, final String[] arguments) {
    super(source, arguments);
  }
}
