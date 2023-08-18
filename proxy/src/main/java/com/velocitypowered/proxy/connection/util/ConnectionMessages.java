/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Common messages that might be sent a client for various connection-related states.
 */
public class ConnectionMessages {

  public static final TranslatableComponent ALREADY_CONNECTED = Component
      .translatable("velocity.error.already-connected", NamedTextColor.RED);
  public static final TranslatableComponent IN_PROGRESS = Component
      .translatable("velocity.error.already-connecting", NamedTextColor.RED);
  public static final TranslatableComponent INTERNAL_SERVER_CONNECTION_ERROR = Component
      .translatable("velocity.error.internal-server-connection-error", NamedTextColor.RED);

  private ConnectionMessages() {
    throw new AssertionError();
  }
}
