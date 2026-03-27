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

package com.velocityctd.proxy.redis.impl.packet;

import com.velocityctd.proxy.redis.packet.typed.UuidPacket;
import java.util.UUID;

/**
 * Represents a packet that sends a player to a remote address.
 */
public final class VelocityRemote extends UuidPacket {

  /**
   * The IP address of the remote server the player should be sent to.
   */
  private final String ip;

  /**
   * The port number of the remote server the player should be sent to.
   */
  private final int port;

  /**
   * Constructs a new {@link VelocityRemote} packet.
   *
   * @param uniqueId the player's unique ID
   * @param ip the IP address of the remote server
   * @param port the port of the remote server
   */
  public VelocityRemote(final UUID uniqueId, final String ip, final int port) {
    super(uniqueId);

    this.ip = ip;
    this.port = port;
  }

  /**
   * Gets the IP address of the remote server.
   *
   * @return the IP address of the remote server
   */
  public String getIp() {
    return ip;
  }

  /**
   * Gets the port of the remote server.
   *
   * @return the port of the remote server
   */
  public int getPort() {
    return port;
  }
}
