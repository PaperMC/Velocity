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

package com.velocitypowered.api.proxy.player;

import com.google.common.base.Preconditions;

/**
 * Represents the handshake sent from the proxy to a server. This class is immutable.
 */
public final class ServerHandshake {

  private final String serverAddress;
  private final int port;

  /**
   * Creates a new server handshake.
   * @param serverAddress the server address
   * @param port the server port
   */
  public ServerHandshake(String serverAddress, int port) {
    this.serverAddress = Preconditions.checkNotNull(serverAddress, "serverAddress");
    this.port = port;
  }

  /**
   * Returns the connection address to send to the server.
   * @return the server address
   */
  public String getServerAddress() {
    return serverAddress;
  }

  /**
   * Returns the connection port to send to the server.
   * @return the port
   */
  public int getPort() {
    return port;
  }

  /**
   * Creates a new {@code ServerHandshake} with the specified server address.
   *
   * @param serverAddress the new server address
   * @return the new {@code ServerHandshake}
   */
  public ServerHandshake withAddress(String serverAddress) {
    return new ServerHandshake(serverAddress, port);
  }

  /**
   * Creates a new {@code ServerHandshake} with the specified port.
   *
   * @param port the new port
   * @return the new {@code ServerHandshake}
   */
  public ServerHandshake withPort(int port) {
    return new ServerHandshake(serverAddress, port);
  }

  @Override
  public String toString() {
    return "ServerHandshake{"
        + "serverAddress='" + serverAddress + '\''
        + ", port=" + port
        + '}';
  }
}
