package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.util.BrigadierUtils;

final class VelocityRawCommandInvocation extends AbstractCommandInvocation<String>
        implements RawCommand.Invocation {

  static final Factory FACTORY = new Factory();

  static class Factory implements CommandInvocationFactory<RawCommand.Invocation> {

    @Override
    public RawCommand.Invocation create(final CommandContext<CommandSource> context) {
      return new VelocityRawCommandInvocation(
              context.getSource(),
              BrigadierUtils.getAlias(context),
              BrigadierUtils.getRawArguments(context));
    }
  }

  private final String alias;

  private VelocityRawCommandInvocation(final CommandSource source,
                               final String alias, final String arguments) {
    super(source, arguments);
    this.alias = Preconditions.checkNotNull(alias);
  }

  @Override
  public String alias() {
    return alias;
  }
}
