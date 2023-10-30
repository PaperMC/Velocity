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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Basic, common command messages.
 */
public class CommandMessages {

  public static final TranslatableComponent PLAYERS_ONLY = Component.translatable(
      "velocity.command.players-only", NamedTextColor.RED);
  public static final TranslatableComponent SERVER_DOES_NOT_EXIST = Component.translatable(
      "velocity.command.server-does-not-exist", NamedTextColor.RED);
  public static final TranslatableComponent PLAYER_NOT_FOUND = Component.translatable(
      "velocity.command.player-not-found", NamedTextColor.RED);
}
