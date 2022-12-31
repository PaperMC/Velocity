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
import com.velocitypowered.api.network.ProtocolVersion;

/**
 * Represents the handshake sent from the proxy to a server. This class is immutable.
 */
public final class ServerHandshake {

  private final ProtocolVersion protocolVersion;
  private final String serverAddress;
  private final int port;
  private final int nextStatus;

  /**
   * Creates a new server handshake.
   * @param protocolVersion the protocol version
   * @param serverAddress the server address
   * @param port the server port
   * @param nextStatus the next status
   */
  public ServerHandshake(ProtocolVersion protocolVersion, String serverAddress, int port,
      int nextStatus) {
    this.protocolVersion = Preconditions.checkNotNull(protocolVersion, "protocolVersion");
    this.serverAddress = Preconditions.checkNotNull(serverAddress, "serverAddress");
    this.port = port;
    this.nextStatus = nextStatus;
  }

  /**
   * Returns the protocol version to send to the server.
   * @return the protocol version
   */
  public ProtocolVersion getProtocolVersion() {
    return protocolVersion;
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
   * Returns the next status to send to the server.
   * @return the next status
   */
  public int getNextStatus() {
    return nextStatus;
  }

  /**
   * Creates a new {@code ServerHandshake} with the specified server address.
   *
   * @param serverAddress the new server address
   * @return the new {@code ServerHandshake}
   */
  public ServerHandshake withAddress(String serverAddress) {
    return new ServerHandshake(protocolVersion, serverAddress, port, nextStatus);
  }

  /**
   * Creates a new {@code ServerHandshake} with the specified port.
   *
   * @param port the new port
   * @return the new {@code ServerHandshake}
   */
  public ServerHandshake withPort(int port) {
    return new ServerHandshake(protocolVersion, serverAddress, port, nextStatus);
  }

  @Override
  public String toString() {
    return "ServerHandshake{"
        + "protocolVersion=" + protocolVersion
        + ", serverAddress='" + serverAddress + '\''
        + ", port=" + port
        + ", nextStatus=" + nextStatus
        + '}';
  }
}
