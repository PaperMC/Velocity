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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Create class for command.
 */
public final class HelpCommand {

  public HelpCommand() {
  }

  /**
     * Creates a Velocity Help Command.
     */
  public static BrigadierCommand create() {
    return new BrigadierCommand(LiteralArgumentBuilder.<CommandSource>literal("velocity:help")
            .requires(source ->
                    source.getPermissionValue("velocity.command.help") == Tristate.TRUE)
                .executes(context -> {
                  final CommandSource source = context.getSource();
                  source.sendMessage(
                          Component.translatable("velocity.command.help.title", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.end", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.glist", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.send-usage", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.server", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.shutdown", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.stop", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.velocity", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.velocity-callback", NamedTextColor.WHITE));
                  source.sendMessage(
                          Component.translatable("velocity.command.help.help", NamedTextColor.WHITE));
                  return Command.SINGLE_SUCCESS;
                }));
  }
}
