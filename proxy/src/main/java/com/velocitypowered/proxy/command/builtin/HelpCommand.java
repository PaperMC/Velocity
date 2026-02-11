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
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.plugin.virtual.VelocityVirtualPlugin;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Implements the {@code /vhelp} command.
 * Provides a list of all registered commands that the user has
 * permission to execute.
 */
public final class HelpCommand {

  private final ProxyServer server;

  public HelpCommand(ProxyServer server) {
    this.server = server;
  }

  /**
   * Registers this command.
   */
  public void register() {
    LiteralArgumentBuilder<CommandSource> root = BrigadierCommand.literalArgumentBuilder("vhelp")
        .executes(context -> {
          CommandSource source = context.getSource();
          List<String> aliases = new ArrayList<>(server.getCommandManager().getAliases());
          Collections.sort(aliases);

          TextComponent.Builder builder = Component.text().append(Component.translatable("velocity.command.vhelp.header").color(NamedTextColor.GOLD));
          
          for (String alias : aliases) {
            if (server.getCommandManager().hasCommand(alias, source)) {
              builder.append(Component.newline())
                  .append(Component.text("- /" + alias, NamedTextColor.YELLOW));
            }
          }
          
          source.sendMessage(builder.build());
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
}
