/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.command.invocation;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.velocitypowered.api.command.CommandInvocation;
import com.velocitypowered.api.command.CommandSource;
import java.util.List;
import java.util.Map;

/**
 * Creates command invocation objects from a command context builder or a command context.
 *
 * <p>Let {@code builder} be a command context builder, and {@code context}
 * a context returned by calling {@link CommandContextBuilder#build(String)} on {@code builder}. The
 * invocations returned by {@link #create(CommandContext)} when given {@code context}, and
 * {@link #create(CommandContextBuilder)} when given {@code builder} are equal.
 *
 * @param <I> the type of the built invocation
 */
public interface CommandInvocationFactory<I extends CommandInvocation<?>> {

  /**
   * Creates an invocation from the given command context.
   *
   * @param context the command context
   * @return the built invocation context
   */
  default I create(final CommandContext<CommandSource> context) {
    return this.create(context.getSource(), context.getNodes(), context.getArguments());
  }

  /**
   * Creates an invocation from the given command context builder.
   *
   * @param context the command context builder
   * @return the built invocation context
   */
  default I create(final CommandContextBuilder<CommandSource> context) {
    return this.create(context.getSource(), context.getNodes(), context.getArguments());
  }

  /**
   * Creates an invocation from the given parsed nodes and arguments.
   *
   * @param source    the command source
   * @param nodes     the list of parsed nodes, as returned by {@link CommandContext#getNodes()} and
   *                  {@link CommandContextBuilder#getNodes()}
   * @param arguments the list of parsed arguments, as returned by
   *                  {@link CommandContext#getArguments()} and
   *                  {@link CommandContextBuilder#getArguments()}
   * @return the built invocation context
   */
  // This provides an abstraction over methods common to CommandContext and CommandContextBuilder.
  // Annoyingly, they mostly have the same getters but one is (correctly) not a subclass of
  // the other. Subclasses may override the methods above to obtain class-specific data.
  I create(final CommandSource source, final List<? extends ParsedCommandNode<?>> nodes,
      final Map<String, ? extends ParsedArgument<?, ?>> arguments);
}
