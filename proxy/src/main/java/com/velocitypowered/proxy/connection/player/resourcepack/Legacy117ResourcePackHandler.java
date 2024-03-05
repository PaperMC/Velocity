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

package com.velocitypowered.proxy.connection.player.resourcepack;

import com.velocitypowered.api.event.player.PlayerResourcePackStatusEvent;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;

/**
 * Legacy (Minecraft 1.17-1.20.2) ResourcePackHandler.
 */
public final class Legacy117ResourcePackHandler extends LegacyResourcePackHandler {
  Legacy117ResourcePackHandler(final ConnectedPlayer player, final VelocityServer server) {
    super(player, server);
  }

  @Override
  protected boolean shouldDisconnectForForcePack(final PlayerResourcePackStatusEvent event) {
    return super.shouldDisconnectForForcePack(event) && !event.isOverwriteKick();
  }
}
