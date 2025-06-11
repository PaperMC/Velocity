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

package com.velocitypowered.proxy.command.builtin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.adventure.ClickCallbackManager;
import java.util.UUID;

/**
 * Callback Command.
 */
public final class CallbackCommand implements Command<CommandSource> {

  @SuppressWarnings("checkstyle:MissingJavadocMethod")
  public static BrigadierCommand create() {
    final LiteralCommandNode<CommandSource> node = BrigadierCommand
        .literalArgumentBuilder("velocity:callback")
        .then(BrigadierCommand.requiredArgumentBuilder("id", StringArgumentType.word())
                .executes(new CallbackCommand()))
        .build();

    return new BrigadierCommand(node);
  }

  @Override
  public int run(final CommandContext<CommandSource> context) {
    final String providedId = StringArgumentType.getString(context, "id");
    final UUID id;
    try {
      id = UUID.fromString(providedId);
    } catch (final IllegalArgumentException ignored) {
      return Command.SINGLE_SUCCESS;
    }

    ClickCallbackManager.INSTANCE.runCallback(context.getSource(), id);
    return Command.SINGLE_SUCCESS;
  }
}
