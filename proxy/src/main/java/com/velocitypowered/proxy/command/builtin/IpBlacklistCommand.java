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
import java.time.Duration;
import java.util.Collection;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Implements the {@code /ipblacklist} command.
 * Allows administrators to manage the IP blacklist, supporting individual
 * IPs, CIDR ranges, temporary bans, and reasons.
 * Permissions:
 * {@code velocity.command.ipblacklist} required to use any sub-command
 */
public final class IpBlacklistCommand {

  private final ProxyServer server;

  public IpBlacklistCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    LiteralArgumentBuilder<CommandSource> root = BrigadierCommand.literalArgumentBuilder("ipblacklist")
        .requires(source -> source.getPermissionValue("velocity.command.ipblacklist") == Tristate.TRUE);

    // add <ip|cidr> [duration] [reason]
    root.then(BrigadierCommand.literalArgumentBuilder("add")
        .then(BrigadierCommand.requiredArgumentBuilder("pattern", StringArgumentType.string())
            .executes(this::addBlacklist)
            .then(BrigadierCommand.requiredArgumentBuilder("duration", StringArgumentType.string())
                .executes(this::addBlacklistWithDuration)
                .then(BrigadierCommand.requiredArgumentBuilder("reason", StringArgumentType.greedyString())
                    .executes(this::addBlacklistFull)))));

    // remove <ip|cidr>
    root.then(BrigadierCommand.literalArgumentBuilder("remove")
        .then(BrigadierCommand.requiredArgumentBuilder("pattern", StringArgumentType.string())
            .executes(this::removeBlacklist)));

    // list
    root.then(BrigadierCommand.literalArgumentBuilder("list")
        .executes(this::listBlacklist));

    BrigadierCommand brigadierCommand = new BrigadierCommand(root);
    server.getCommandManager().register(
        server.getCommandManager().metaBuilder(brigadierCommand)
            .plugin(VelocityVirtualPlugin.INSTANCE)
            .build(),
        brigadierCommand
    );
  }

  private int addBlacklist(CommandContext<CommandSource> context) {
    return add(context, null, null);
  }

  private int addBlacklistWithDuration(CommandContext<CommandSource> context) {
    return add(context, context.getArgument("duration", String.class), null);
  }

  private int addBlacklistFull(CommandContext<CommandSource> context) {
    return add(context, context.getArgument("duration", String.class), context.getArgument("reason", String.class));
  }

  private int add(CommandContext<CommandSource> context, String durationStr, String reason) {
    String pattern = context.getArgument("pattern", String.class);
    Duration duration = null;
    if (durationStr != null) {
      try {
        duration = parseDuration(durationStr);
      } catch (IllegalArgumentException e) {
        context.getSource().sendMessage(Component.translatable("velocity.command.ipblacklist.invalid-duration").color(NamedTextColor.RED));
        return -1;
      }
    }

    try {
      if (pattern.contains("/")) {
        server.getIpFilterManager().blacklistCidr(pattern, duration, reason);
      } else {
        server.getIpFilterManager().blacklist(InetAddress.getByName(pattern), duration, reason);
      }
      context.getSource().sendMessage(Component.translatable("velocity.command.ipblacklist.added",
          Component.text(pattern)).color(NamedTextColor.GREEN));
    } catch (Exception e) {
      context.getSource().sendMessage(Component.translatable("velocity.command.ipblacklist.error",
          Component.text(pattern), Component.text(String.valueOf(e.getMessage()))).color(NamedTextColor.RED));
      return -1;
    }
    return Command.SINGLE_SUCCESS;
  }

  private int removeBlacklist(CommandContext<CommandSource> context) {
    String pattern = context.getArgument("pattern", String.class);
    boolean removed;
    try {
      if (pattern.contains("/")) {
        removed = server.getIpFilterManager().unblacklistCidr(pattern);
      } else {
        removed = server.getIpFilterManager().unblacklist(InetAddress.getByName(pattern));
      }
      if (removed) {
        context.getSource().sendMessage(Component.translatable("velocity.command.ipblacklist.removed",
            Component.text(pattern)).color(NamedTextColor.GREEN));
      } else {
        context.getSource().sendMessage(Component.translatable("velocity.command.ipblacklist.not-present",
            Component.text(pattern)).color(NamedTextColor.YELLOW));
      }
    } catch (Exception e) {
      context.getSource().sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
      return -1;
    }
    return Command.SINGLE_SUCCESS;
  }

  private int listBlacklist(CommandContext<CommandSource> context) {
    Collection<IpFilterEntry> entries = server.getIpFilterManager().getBlacklistEntries();
    if (entries.isEmpty()) {
      context.getSource().sendMessage(Component.translatable("velocity.command.ipblacklist.empty").color(NamedTextColor.YELLOW));
      return Command.SINGLE_SUCCESS;
    }

    TextComponent.Builder builder = Component.text().append(Component.translatable("velocity.command.ipblacklist.header").color(NamedTextColor.GOLD));
    for (IpFilterEntry entry : entries) {
      builder.append(Component.newline())
          .append(Component.text("- ", NamedTextColor.YELLOW))
          .append(Component.text(entry.getPattern(), NamedTextColor.YELLOW));
      entry.getReason().ifPresent(r -> builder.append(Component.space())
          .append(Component.translatable("velocity.command.ipfilter.reason", Component.text(r)).color(NamedTextColor.GRAY)));
      entry.getExpiration().ifPresent(e -> builder.append(Component.space())
          .append(Component.translatable("velocity.command.ipfilter.expires", Component.text(e.toString())).color(NamedTextColor.GRAY)));
    }
    context.getSource().sendMessage(builder.build());
    return Command.SINGLE_SUCCESS;
  }

  private Duration parseDuration(String input) {
    if (input == null || input.isEmpty()) {
      return null;
    }
    char unit = input.charAt(input.length() - 1);
    long value = Long.parseLong(input.substring(0, input.length() - 1));
    return switch (unit) {
      case 's' -> Duration.ofSeconds(value);
      case 'm' -> Duration.ofMinutes(value);
      case 'h' -> Duration.ofHours(value);
      case 'd' -> Duration.ofDays(value);
      default -> throw new IllegalArgumentException("Unknown unit: " + unit);
    };
  }
}
