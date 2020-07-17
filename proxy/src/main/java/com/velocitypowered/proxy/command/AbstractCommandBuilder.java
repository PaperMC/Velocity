package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.Command;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for {@link Command.Builder} implementations.
 *
 * @param <T> the type of the registered command
 * @param <B> the type of this builder (used for chaining)
 */
abstract class AbstractCommandBuilder<T extends Command<?>, B extends Command.Builder<T, B>>
        implements Command.Builder<T, B> {

  protected final VelocityCommandManager manager;
  protected final Set<String> aliases = new HashSet<>();

  protected AbstractCommandBuilder(final VelocityCommandManager manager) {
    this.manager = Preconditions.checkNotNull(manager, "manager");
  }

  @Override
  public B aliases(String... aliases) {
    Preconditions.checkNotNull(aliases);

    for (int i = 0, length = aliases.length; i < length; i++) {
      final String alias1 = aliases[i];
      Preconditions.checkNotNull(alias1, "alias at index %s", i);
      this.aliases.add(alias1);
    }

    return self();
  }

  /**
   * Returns this instance.
   *
   * @return this instance
   */
  protected abstract B self();
}
