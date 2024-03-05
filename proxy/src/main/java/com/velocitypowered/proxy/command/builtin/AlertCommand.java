/*
 * Copyright (C) 2018-2024 Velocity Contributors
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
import com.velocitypowered.api.proxy.server.RegisteredServer;
import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Command that broadcasts the given message to all the servers on the proxy.
 */
public class AlertCommand {
  private final ProxyServer server;

  private static final Map<String, String> colorMap = new HashMap<>();

  static {
    colorMap.put("&0", "<black>");
    colorMap.put("&1", "<dark_blue>");
    colorMap.put("&2", "<dark_green>");
    colorMap.put("&3", "<dark_aqua>");
    colorMap.put("&4", "<dark_red>");
    colorMap.put("&5", "<dark_purple>");
    colorMap.put("&6", "<gold>");
    colorMap.put("&7", "<gray>");
    colorMap.put("&8", "<dark_gray>");
    colorMap.put("&9", "<blue>");
    colorMap.put("&a", "<green>");
    colorMap.put("&b", "<aqua>");
    colorMap.put("&c", "<red>");
    colorMap.put("&d", "<light_purple>");
    colorMap.put("&e", "<yellow>");
    colorMap.put("&f", "<white>");
    colorMap.put("&l", "<bold>");
  }

  public AlertCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Register the command.
   */
  public void register() {
    final LiteralArgumentBuilder<CommandSource> rootNode = BrigadierCommand
        .literalArgumentBuilder("alert")
        .requires(source ->
            source.getPermissionValue("velocity.command.alert") == Tristate.TRUE)
        .executes(this::usage)
        .then(BrigadierCommand
            .requiredArgumentBuilder("message", StringArgumentType.greedyString())
            .executes(this::alert));
    server.getCommandManager().register(new BrigadierCommand(rootNode.build()));
  }


  private int usage(final CommandContext<CommandSource> context) {
    context.getSource().sendMessage(
        Component.translatable("velocity.command.alert.usage", NamedTextColor.YELLOW)
    );
    return Command.SINGLE_SUCCESS;
  }

  private int alert(final CommandContext<CommandSource> context) {
    String message = StringArgumentType.getString(context, "message");
    if (message.isEmpty()) {
      context.getSource().sendMessage(
          Component.translatable("velocity.command.alert.no-message", NamedTextColor.YELLOW)
      );
      return 0;
    }

    for (String s : colorMap.keySet()) {
      message = message.replace(s, colorMap.get(s));
    }

    for (RegisteredServer s : server.getAllServers()) {
      s.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    return Command.SINGLE_SUCCESS;
  }

}
