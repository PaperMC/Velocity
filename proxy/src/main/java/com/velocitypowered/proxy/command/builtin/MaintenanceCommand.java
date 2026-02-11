/*
 * Copyright (C) 2018-2026 Velocity Contributors
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
import com.mojang.brigadier.context.CommandContext;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Implements the {@code /maintenance} command.
 * Allows administrators to enable/disable maintenance mode and set a
 * custom maintenance message.
 * Permissions:
 * {@code velocity.command.maintenance} required to use any sub-command
 */
public final class MaintenanceCommand {

  private final ProxyServer server;

  public MaintenanceCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    LiteralArgumentBuilder<CommandSource> root = BrigadierCommand.literalArgumentBuilder("maintenance")
        .requires(source -> source.getPermissionValue("velocity.command.maintenance") == Tristate.TRUE);

    root.then(BrigadierCommand.literalArgumentBuilder("on")
        .executes(this::enableMaintenance)
        .then(BrigadierCommand.requiredArgumentBuilder("message", StringArgumentType.greedyString())
            .executes(this::enableMaintenanceWithMessage)));

    root.then(BrigadierCommand.literalArgumentBuilder("off")
        .executes(this::disableMaintenance));

    root.executes(context -> {
      boolean enabled = server.getIpFilterManager().isMaintenanceMode();
      if (enabled) {
        context.getSource().sendMessage(Component.translatable("velocity.command.maintenance.status-enabled").color(NamedTextColor.GOLD));
      } else {
        context.getSource().sendMessage(Component.translatable("velocity.command.maintenance.status-disabled").color(NamedTextColor.GREEN));
      }
      return Command.SINGLE_SUCCESS;
    });

    BrigadierCommand brigadierCommand = new BrigadierCommand(root);
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder(brigadierCommand)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        brigadierCommand
    );
  }

  private int enableMaintenance(CommandContext<CommandSource> context) {
    server.getIpFilterManager().setMaintenanceMode(true);
    context.getSource().sendMessage(Component.translatable("velocity.command.maintenance.enabled").color(NamedTextColor.GOLD));
    return Command.SINGLE_SUCCESS;
  }

  private int enableMaintenanceWithMessage(CommandContext<CommandSource> context) {
    String message = context.getArgument("message", String.class);
    server.getIpFilterManager().setMaintenanceMode(true);
    server.getIpFilterManager().setMaintenanceMessage(MiniMessage.miniMessage().deserialize(message));
    context.getSource().sendMessage(Component.translatable("velocity.command.maintenance.enabled-with-message").color(NamedTextColor.GOLD));
    return Command.SINGLE_SUCCESS;
  }

  private int disableMaintenance(CommandContext<CommandSource> context) {
    server.getIpFilterManager().setMaintenanceMode(false);
    context.getSource().sendMessage(Component.translatable("velocity.command.maintenance.disabled").color(NamedTextColor.GREEN));
    return Command.SINGLE_SUCCESS;
  }
}
