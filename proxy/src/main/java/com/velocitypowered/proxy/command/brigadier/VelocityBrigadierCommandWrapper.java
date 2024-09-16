/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.command.brigadier;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.velocitypowered.api.command.CommandSource;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wraps a Brigadier command to allow us to track the registrant.
 */
public class VelocityBrigadierCommandWrapper implements Command<CommandSource> {

  private final Command<CommandSource> delegate;
  private final Object registrant;

  private VelocityBrigadierCommandWrapper(Command<CommandSource> delegate, Object registrant) {
    this.delegate = delegate;
    this.registrant = registrant;
  }

  /**
   * Transforms the given command into a {@code VelocityBrigadierCommandWrapper} if the registrant
   * is not null and if the command is not already wrapped.
   *
   * @param delegate the command to wrap
   * @param registrant the registrant of the command
   * @return the wrapped command, if necessary
   */
  public static Command<CommandSource> wrap(Command<CommandSource> delegate, @Nullable Object registrant) {
    if (registrant == null) {
      // nothing to wrap
      return delegate;
    }
    if (delegate instanceof VelocityBrigadierCommandWrapper) {
      // already wrapped
      return delegate;
    }
    return new VelocityBrigadierCommandWrapper(delegate, registrant);
  }

  @Override
  public int run(CommandContext<CommandSource> context) throws CommandSyntaxException {
    return delegate.run(context);
  }

  public Object registrant() {
    return registrant;
  }
}
