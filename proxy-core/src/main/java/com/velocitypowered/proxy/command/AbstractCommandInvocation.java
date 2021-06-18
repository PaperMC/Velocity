/*
 * Copyright (C) 2018 Velocity Contributors
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

package com.velocitypowered.proxy.command;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;

/**
 * Abstract base class for {@link CommandInvocation} implementations.
 *
 * @param <T> the type of the arguments
 */
abstract class AbstractCommandInvocation<T> implements CommandInvocation<T> {

  private final CommandSource source;
  private final T arguments;

  protected AbstractCommandInvocation(final CommandSource source, final T arguments) {
    this.source = Preconditions.checkNotNull(source, "source");
    this.arguments = Preconditions.checkNotNull(arguments, "arguments");
  }

  @Override
  public CommandSource source() {
    return source;
  }

  @Override
  public T arguments() {
    return arguments;
  }
}
