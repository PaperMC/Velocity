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

package com.velocitypowered.proxy.network.registry.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;

/**
 * A flat protocol registry that does not care about the protocol version.
 */
public class SimpleProtocolRegistry implements ProtocolRegistry {

  private final PacketRegistryMap serverbound;
  private final PacketRegistryMap clientbound;

  public SimpleProtocolRegistry(
      PacketRegistryMap serverbound,
      PacketRegistryMap clientbound) {
    this.serverbound = serverbound;
    this.clientbound = clientbound;
  }

  @Override
  public PacketRegistryMap lookup(PacketDirection direction, ProtocolVersion version) {
    if (direction == PacketDirection.SERVERBOUND) {
      return this.serverbound;
    } else if (direction == PacketDirection.CLIENTBOUND) {
      return this.clientbound;
    } else {
      throw new NullPointerException("direction");
    }
  }
}
