/*
 * Copyright (C) 2020-2023 Velocity Contributors
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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

/**
 * Shuts down the proxy.
 */
public final class ShutdownCommand {

  private ShutdownCommand() {
  }

  /**
   * Creates a Velocity Shutdown Command.
   *
   * @param server the proxy instance
   * @return the Shutdown Command
   */
  public static BrigadierCommand command(final VelocityServer server) {
    return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("shutdown")
        .requires(source -> source == server.getConsoleCommandSource())
        .executes(context -> {
          server.shutdown(true);
          return Command.SINGLE_SUCCESS;
        })
        .then(RequiredArgumentBuilder.<CommandSource, String>argument("reason",
                StringArgumentType.greedyString())
            .executes(context -> {
              String reason = context.getArgument("reason", String.class);
              server.shutdown(true, MiniMessage.miniMessage().deserialize(
                  MiniMessage.miniMessage().serialize(
                      LegacyComponentSerializer.legacy('&').deserialize(reason)
                  )
              ));
              return Command.SINGLE_SUCCESS;
            })
        ).build());
  }
}
