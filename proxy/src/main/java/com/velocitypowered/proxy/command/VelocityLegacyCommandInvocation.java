package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.LegacyCommand;
import java.util.Arrays;

final class VelocityLegacyCommandInvocation extends AbstractCommandInvocation<String[]>
        implements LegacyCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static String[] split(final String line) {
    if (line.isEmpty()) {
      return new String[0];
    }

    String[] trimmed = line.trim().split(" ", -1);
    if (line.endsWith(" ") && !line.trim().isEmpty()) {
      // To work around a 1.13+ specific bug we have to inject a space at the end of the arguments
      trimmed = Arrays.copyOf(trimmed, trimmed.length + 1);
      trimmed[trimmed.length - 1] = "";
    }
    return trimmed;
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
