package com.velocitypowered.proxy.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.BrigadierCommandExecutionContext;
import com.velocitypowered.api.command.CommandSource;
import java.util.Arrays;
import java.util.List;

final class VelocityBrigadierCommand extends AbstractCommand<BrigadierCommandExecutionContext>
        implements BrigadierCommand {

  // This is mostly going to be a dummy class.
  // Ignore the commented code below.

  VelocityBrigadierCommand(final VelocityCommandManager manager, final CommandNode<CommandSource> node) {
    super(manager, node);
  }

  /*@Override
  public void execute(final BrigadierCommandExecutionContext context) {
    try {
      manager.getDispatcher().execute(context.parsed());
    } catch (final CommandSyntaxException e) {
      throw new RuntimeException("Context parsed syntax is invalid", e);
    }
  }

  @Override
  public List<String> suggest(final BrigadierCommandExecutionContext context) {
    return Arrays.asList(manager.getDispatcher().getAllUsage(node, context.source(), true));
  }

  @Override
  public boolean hasPermission(final BrigadierCommandExecutionContext context) {
    return node.canUse(context.source());
  }*/

  //final static class Builder extends

  /*final static class Builder extends AbstractCommandBuilder<BrigadierCommand, BrigadierCommand.Builder>
          implements BrigadierCommand.Builder {

    Builder(final VelocityCommandManager manager) {
      super(manager);
    }

    @Override
    public BrigadierCommand register(final LiteralArgumentBuilder<CommandSource> builder) {
      Preconditions.checkNotNull(builder);
      return register(builder.build());
    }

    @Override
    public BrigadierCommand register(final CommandNode<CommandSource> node) {
      Preconditions.checkNotNull(node);
      final String alias = node.getName().toLowerCase(Locale.ENGLISH);
      manager.registerNode(node);

      aliases.remove(alias); // prevent self-redirect
      for (final String alias1 : aliases) {
        CommandNode<CommandSource> aliasNode = VelocityCommandManager.createRedirectNode(node, alias1);
        manager.registerNode(aliasNode);
      }

      return new VelocityBrigadierCommand(manager, node);
    }

    @Override
    protected Builder self() {
      return this;
    }
  }*/
}
