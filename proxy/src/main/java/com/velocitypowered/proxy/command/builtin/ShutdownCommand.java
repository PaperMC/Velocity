/*
 * Copyright (C) 2018 Velocity Contributors
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

import com.velocitypowered.api.command.RawCommand;
import com.velocitypowered.proxy.VelocityServer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class ShutdownCommand implements RawCommand {

  private final VelocityServer server;

  public ShutdownCommand(VelocityServer server) {
    this.server = server;
  }

  @Override
  public void execute(final Invocation invocation) {
    String reason = invocation.arguments();
    if (reason.isEmpty() || reason.trim().isEmpty()) {
      server.shutdown(true);
    } else {
      server.shutdown(true, LegacyComponentSerializer.legacy('&').deserialize(reason));
    }
  }

  @Override
  public boolean hasPermission(final Invocation invocation) {
    return invocation.source() == server.getConsoleCommandSource();
  }
}
