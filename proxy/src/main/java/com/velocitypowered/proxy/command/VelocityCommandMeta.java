package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.mojang.brigadier.tree.CommandNode;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class VelocityCommandMeta implements CommandMeta {

  static final class Builder implements CommandMeta.Builder {

    private final ImmutableSet.Builder<String> aliases;
    private final ImmutableList.Builder<CommandNode<CommandSource>> hints;

    public Builder(final String alias) {
      Preconditions.checkNotNull(alias, "alias");
      this.aliases = ImmutableSet.<String>builder()
              .add(alias.toLowerCase(Locale.ENGLISH));
      this.hints = ImmutableList.builder();
    }

    @Override
    public CommandMeta.Builder aliases(final String... aliases) {
      Preconditions.checkNotNull(aliases, "aliases");
      for (int i = 0, length = aliases.length; i < length; i++) {
        final String alias1 = aliases[i];
        Preconditions.checkNotNull(alias1, "alias at index %s", i);
        this.aliases.add(alias1.toLowerCase(Locale.ENGLISH));
      }
      return this;
    }

    @Override
    public CommandMeta.Builder hint(final CommandNode<CommandSource> node) {
      Preconditions.checkNotNull(node, "node");
      hints.add(node);
      return this;
    }

    @Override
    public CommandMeta build() {
      return new VelocityCommandMeta(aliases.build(), hints.build());
    }
  }

  private final Set<String> aliases;
  private final List<CommandNode<CommandSource>> hints;

  private VelocityCommandMeta(
          final Set<String> aliases, final List<CommandNode<CommandSource>> hints) {
    this.aliases = aliases;
    this.hints = hints;
  }

  @Override
  public Collection<String> getAliases() {
    return aliases;
  }

  @Override
  public Collection<CommandNode<CommandSource>> getHints() {
    return hints;
  }
}
