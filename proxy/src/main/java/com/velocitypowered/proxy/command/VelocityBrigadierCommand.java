package com.velocitypowered.proxy.command;

import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.BrigadierCommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import java.util.List;

final class VelocityBrigadierCommand implements BrigadierCommand {

  private final VelocityCommandManager manager;
  private final CommandNode<CommandSource> node;

  private VelocityBrigadierCommand(final VelocityCommandManager manager, final CommandNode<CommandSource> node) {
    this.manager = manager;
    this.node = node;
  }

  @Override
  public void execute(final BrigadierCommandInvocation invocation) {
    try {
      manager.getDispatcher().execute(invocation.parsed());
    } catch (final CommandSyntaxException e) {
      throw new RuntimeException("Valid parse results threw syntax exception", e);
    }
  }

  @Override
  public List<String> suggest(final BrigadierCommandInvocation invocation) {
    // TODO Add when async suggestions are added back
    //return manager.getDispatcher().getCompletionSuggestions()
    return null;
  }

  @Override
  public boolean hasPermission(final BrigadierCommandInvocation invocation) {
    for (ParsedCommandNode<CommandSource> node : invocation.parsed().getContext().getNodes()) {
      if (!node.getNode().canUse(invocation.source())) {
        return false;
      }
    }

    return true;
  }

  // This is mostly going to be a dummy class.
  // Ignore the commented code below.

  /*
  @Override
  public List<String> suggest(final BrigadierCommandExecutionContext context) {
    return Arrays.asList(manager.getDispatcher().getAllUsage(node, context.source(), true));
  }

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
