/*
 * Copyright (C) 2021-2023 Velocity Contributors
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

package com.velocitypowered.proxy.config.server;

import com.velocitypowered.api.proxy.config.BackendServerConfig;
import com.velocitypowered.api.proxy.server.ServerInfoForwardingMode;
import java.util.Objects;

/**
 * The implementation of BackedServerConfig interface.
 */
public class BackendServerConfigImpl implements BackendServerConfig {

  /**
   * The address of the backend server.
   */
  private String address;

  /**
   * The forwarding mode of the backend server.
   */
  private ServerInfoForwardingMode forwardingMode;

  private BackendServerConfigImpl() {}

  public BackendServerConfigImpl(String address, ServerInfoForwardingMode forwardingMode) {
    this.address = address;
    this.forwardingMode = forwardingMode;
  }

  public BackendServerConfigImpl(String address) {
    this.address = address;
    this.forwardingMode = ServerInfoForwardingMode.FOLLOWUP;
  }

  public String getAddress() {
    return address;
  }

  public ServerInfoForwardingMode getForwardingMode() {
    return forwardingMode;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public  void setForwardingMode(ServerInfoForwardingMode forwardingMode) {
    this.forwardingMode = forwardingMode;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof BackendServerConfigImpl that)) {
      return false;
    }
    return Objects.equals(address, that.address) && forwardingMode == that.forwardingMode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(address, forwardingMode);
  }

  @Override
  public String toString() {
    return "BackendServerConfig{"
            + "address='"
            + address
            + '\''
            + ", forwardingMode="
            + forwardingMode
            + '}';
  }
}
