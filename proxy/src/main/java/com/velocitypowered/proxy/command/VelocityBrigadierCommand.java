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

  // CommandNodes may only have one alias. Brigadier provides
  // a redirection system that allows forwarding interactions
  // to be forwarded to the "main" command node (e.g. /tp -> /teleport).
  //
  // These can be created using LiteralArgumentBuilder.redirect().
  // However, as noted on https://github.com/Mojang/brigadier/issues/46
  // redirects currently only work for command nodes that contain
  // children nodes (i.e. have variants that expect arguments).
  // Given a redirect of an argument-less command such as /shutdown
  // incorrectly throw CommandSyntaxException on execution.
  //
  // This method checks if the redirection destination has children.
  // If so, it performs a regular redirect. Otherwise, it creates
  // a manual forward, preserving the permission predicate and
  // tab complete suggestions.

  /**
   * Returns a node builder with the given alias to the specified destination node.
   *
   * @param dest the destination node
   * @param alias the command alias
   * @return the created alias
   */
  private static LiteralArgumentBuilder<CommandSource> redirect(
          final CommandNode<CommandSource> dest, String alias) {
    Preconditions.checkNotNull(dest, "dest");
    Preconditions.checkNotNull(alias, "alias");
    alias = alias.toLowerCase(Locale.ENGLISH);
    Preconditions.checkArgument(dest.getName().equals(alias),
            "Self-referencing redirect %s for %s", alias, dest);

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
    public BrigadierCommand register(final CommandNode<CommandSource> node) {
      Preconditions.checkNotNull(node, "node");
      final BrigadierCommand command = new VelocityBrigadierCommand(manager, node);
      final String alias = node.getName().toLowerCase(Locale.ENGLISH);
      manager.register(alias, command);

      aliases.remove(alias); // prevent self-redirect
      for (final String alias1 : aliases) {
        manager.getBrigadierDispatcher().register(redirect(node, alias1));
      }

      return command;
    }

    @Override
    protected BrigadierCommand.Builder self() {
      return this;
    }
  }

  private final VelocityCommandManager manager;
  private final CommandNode<CommandSource> node;

  private VelocityBrigadierCommand(
          final VelocityCommandManager manager, final CommandNode<CommandSource> node) {
    this.manager = manager;
    this.node = node;
  }

  @Override
  public void execute(final BrigadierCommandInvocation invocation) {
    try {
      manager.getBrigadierDispatcher().execute(invocation.parsed());
    } catch (final CommandSyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public CompletableFuture<List<String>> suggestAsync(final BrigadierCommandInvocation invocation) {
    return manager.getBrigadierDispatcher().getCompletionSuggestions(invocation.parsed())
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
}
