package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.util.BrigadierUtils;
import java.util.function.Predicate;

final class VelocityBrigadierCommand implements BrigadierCommand {

  static final class Builder
          extends AbstractCommandBuilder<BrigadierCommand, BrigadierCommand.Builder>
          implements BrigadierCommand.Builder {

    private Predicate<CommandContext<CommandSource>> permissionPredicate;

    Builder(final VelocityCommandManager manager) {
      super(manager);
    }

    @Override
    public BrigadierCommand.Builder permission(
            final Predicate<CommandContext<CommandSource>> predicate) {
      this.permissionPredicate = predicate;
      return self();
    }

    @Override
    public BrigadierCommand register(final LiteralArgumentBuilder<CommandSource> builder) {
      Preconditions.checkNotNull(builder, "builder");
      return register(builder.build());
    }

    @Override
    public BrigadierCommand register(LiteralCommandNode<CommandSource> node) {
      Preconditions.checkNotNull(node, "node");
      if (permissionPredicate != null) {
        // Executes the command iff the permission predicate passes. Redirect nodes
        // don't require the wrapping since they copy the destination node's command.
        node = (LiteralCommandNode<CommandSource>) BrigadierUtils.wrapWithContextPredicate(
                node, permissionPredicate, BrigadierUtils.NO_PERMISSION);
      }

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
