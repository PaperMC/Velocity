package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;

final class VelocityBrigadierCommand implements BrigadierCommand {

  static final class Builder
          extends AbstractCommandBuilder<BrigadierCommand, BrigadierCommand.Builder>
          implements BrigadierCommand.Builder {

    Builder(final VelocityCommandManager manager) {
      super(manager);
    }

    @Override
    public BrigadierCommand register(final LiteralArgumentBuilder<CommandSource> builder) {
      Preconditions.checkNotNull(builder, "builder");
      return register(builder.build());
    }

    @Override
    public BrigadierCommand register(final LiteralCommandNode<CommandSource> node) {
      Preconditions.checkNotNull(node, "node");

      VelocityBrigadierCommand command = new VelocityBrigadierCommand(node);
      getManager().register(node.getName(), command, getAliases().toArray(String[]::new));
      return command;
    }

    @Override
    protected BrigadierCommand.Builder self() {
      return this;
    }
  }

  private final LiteralCommandNode<CommandSource> node;

  private VelocityBrigadierCommand(final LiteralCommandNode<CommandSource> node) {
    this.node = node;
  }

  public LiteralCommandNode<CommandSource> getNode() {
    return node;
  }
}
