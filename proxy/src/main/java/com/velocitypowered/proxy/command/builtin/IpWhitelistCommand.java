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
import com.velocitypowered.api.proxy.filter.IpFilterManager.IpFilterEntry;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.net.InetAddress;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Implements the {@code /ipwhitelist} command.
 * Allows administrators to manage the IP whitelist. Whitelisted IPs can
 * still connect even when maintenance mode is enabled.
 * Permissions:
 * {@code velocity.command.ipwhitelist} required to use any sub-command
 */
public final class IpWhitelistCommand {

  private final ProxyServer server;

  public IpWhitelistCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    LiteralArgumentBuilder<CommandSource> root = BrigadierCommand.literalArgumentBuilder("ipwhitelist")
        .requires(source -> source.getPermissionValue("velocity.command.ipwhitelist") == Tristate.TRUE);

    // add <ip|cidr>
    root.then(BrigadierCommand.literalArgumentBuilder("add")
        .then(BrigadierCommand.requiredArgumentBuilder("pattern", StringArgumentType.string())
            .executes(this::addWhitelist)));

    // remove <ip|cidr>
    root.then(BrigadierCommand.literalArgumentBuilder("remove")
        .then(BrigadierCommand.requiredArgumentBuilder("pattern", StringArgumentType.string())
            .executes(this::removeWhitelist)));

    // list
    root.then(BrigadierCommand.literalArgumentBuilder("list")
        .executes(this::listWhitelist));

    BrigadierCommand brigadierCommand = new BrigadierCommand(root);
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder(brigadierCommand)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        brigadierCommand
    );
  }

  private int addWhitelist(CommandContext<CommandSource> context) {
    String pattern = context.getArgument("pattern", String.class);
    try {
      if (pattern.contains("/")) {
        server.getIpFilterManager().whitelistCidr(pattern);
      } else {
        server.getIpFilterManager().whitelist(InetAddress.getByName(pattern));
      }
      context.getSource().sendMessage(Component.translatable("velocity.command.ipwhitelist.added",
          Component.text(pattern)).color(NamedTextColor.GREEN));
    } catch (Exception e) {
      context.getSource().sendMessage(Component.translatable("velocity.command.ipwhitelist.error",
          Component.text(pattern), Component.text(String.valueOf(e.getMessage()))).color(NamedTextColor.RED));
      return -1;
    }
    return Command.SINGLE_SUCCESS;
  }

  private int removeWhitelist(CommandContext<CommandSource> context) {
    String pattern = context.getArgument("pattern", String.class);
    boolean removed;
    try {
      if (pattern.contains("/")) {
        removed = server.getIpFilterManager().unwhitelistCidr(pattern);
      } else {
        removed = server.getIpFilterManager().unwhitelist(InetAddress.getByName(pattern));
      }
      if (removed) {
        context.getSource().sendMessage(Component.translatable("velocity.command.ipwhitelist.removed",
            Component.text(pattern)).color(NamedTextColor.GREEN));
      } else {
        context.getSource().sendMessage(Component.translatable("velocity.command.ipwhitelist.not-present",
            Component.text(pattern)).color(NamedTextColor.YELLOW));
      }
    } catch (Exception e) {
      context.getSource().sendMessage(Component.translatable("velocity.command.ipwhitelist.error",
          Component.text(pattern), Component.text(String.valueOf(e.getMessage()))).color(NamedTextColor.RED));
      return -1;
    }
    return Command.SINGLE_SUCCESS;
  }

  private int listWhitelist(CommandContext<CommandSource> context) {
    Collection<IpFilterEntry> entries = server.getIpFilterManager().getWhitelistEntries();
    if (entries.isEmpty()) {
      context.getSource().sendMessage(Component.translatable("velocity.command.ipwhitelist.empty").color(NamedTextColor.YELLOW));
      return Command.SINGLE_SUCCESS;
    }

    TextComponent.Builder builder = Component.text().append(Component.translatable("velocity.command.ipwhitelist.header").color(NamedTextColor.GOLD));
    for (IpFilterEntry entry : entries) {
      builder.append(Component.newline())
          .append(Component.text("- ", NamedTextColor.YELLOW))
          .append(Component.text(entry.getPattern(), NamedTextColor.YELLOW));
    }
    context.getSource().sendMessage(builder.build());
    return Command.SINGLE_SUCCESS;
  }
}
