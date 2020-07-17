package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.BrigadierCommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

final class VelocityBrigadierCommand implements BrigadierCommand {

  /**
   * Returns a node builder with the given alias to the specified destination node.
   *
   * @param dest the destination node
   * @param alias the command alias
   * @return the created alias
   * @see <a href="https://github.com/Mojang/brigadier/issues/46">issue</a>
   */
  private static LiteralArgumentBuilder<CommandSource> redirect(final CommandNode<CommandSource> dest, String alias) {
    Preconditions.checkNotNull(dest, "dest");
    Preconditions.checkNotNull(alias, "alias");
    alias = alias.toLowerCase(Locale.ENGLISH);
    Preconditions.checkArgument(dest.getName().equals(alias),"Self-referencing redirect %s for %s", alias, dest);

    if (!dest.getChildren().isEmpty()) {
      // Regular redirects work if the destination node can expect arguments.
      return BrigadierCommand.argumentBuilder(alias).redirect(dest);
    }

    // See LiteralCommandNode.createBuilder
    final LiteralArgumentBuilder<CommandSource> builder = BrigadierCommand.argumentBuilder(alias);
    builder.requires(dest.getRequirement());
    builder.forward(dest.getRedirect(), dest.getRedirectModifier(), dest.isFork());
    if (dest.getCommand() != null) {
      builder.executes(dest.getCommand());
    }
    return builder;
  }

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
  public CompletableFuture<List<String>> suggestAsync(final BrigadierCommandInvocation invocation) {
    return manager.getDispatcher().getCompletionSuggestions(invocation.parsed())
            // The client infers the position of the suggestions
            .thenApply(suggestions -> Lists.transform(suggestions.getList(), Suggestion::getText));
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

  CommandNode<CommandSource> getNode() {
    return node;
  }

  final static class Builder extends AbstractCommandBuilder<BrigadierCommand, BrigadierCommand.Builder>
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
    public BrigadierCommand register(final CommandNode<CommandSource> node) {
      Preconditions.checkNotNull(node, "node");
      final BrigadierCommand command = new VelocityBrigadierCommand(manager, node);
      final String alias = node.getName().toLowerCase(Locale.ENGLISH);
      manager.register(alias, command);

      aliases.remove(alias); // prevent self-redirect
      for (final String alias1 : aliases) {
        manager.getDispatcher().register(redirect(node, alias1));
      }

      return command;
    }

    @Override
    protected BrigadierCommand.Builder self() {
      return this;
    }
  }
}
