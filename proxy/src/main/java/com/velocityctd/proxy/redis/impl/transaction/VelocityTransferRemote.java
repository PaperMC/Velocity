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

package com.velocityctd.proxy.redis.impl.transaction;

import com.velocityctd.proxy.redis.impl.PacketBehaviour;
import com.velocityctd.proxy.redis.impl.packet.VelocityRemote;
import com.velocityctd.proxy.redis.packet.annotation.OneWayPacket;
import com.velocityctd.proxy.redis.packet.typed.ComponentPacket;
import com.velocitypowered.api.command.CommandSource;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a transaction that transfers a player to a remote server.
 */
@OneWayPacket
public final class VelocityTransferRemote extends VelocityTransaction<VelocityRemote, ComponentPacket> {

  /**
   * Constructs a new {@link VelocityTransferRemote} transaction.
   *
   * @param source the command source to send the result to
   * @param uniqueId the player's unique ID
   * @param ip the IP address of the remote server
   * @param port the port of the remote server
   */
  public VelocityTransferRemote(final @NotNull CommandSource source, final UUID uniqueId, final String ip, final int port) {
    super(new VelocityRemote(uniqueId, ip, port), source, "redis.command.transfer.timeout");

    this.onComplete(packet -> PacketBehaviour.SEND_COMPONENT.behave(source, packet));
  }
}
