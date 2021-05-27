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
import com.velocitypowered.proxy.network.registry.packet.EmptyPacketRegistryMap;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * A version-aware protocol registry.
 */
public class VersionSpecificProtocolRegistry implements ProtocolRegistry {

  private final Map<ProtocolVersion, PacketRegistryMap> serverboundByVersion;
  private final Map<ProtocolVersion, PacketRegistryMap> clientboundByVersion;

  public VersionSpecificProtocolRegistry() {
    this.serverboundByVersion = new EnumMap<>(ProtocolVersion.class);
    this.clientboundByVersion = new EnumMap<>(ProtocolVersion.class);
  }

  public VersionSpecificProtocolRegistry register(ProtocolVersion min, ProtocolVersion max,
      PacketRegistryMap serverbound, PacketRegistryMap clientbound) {
    for (ProtocolVersion version : EnumSet.range(min, max)) {
      this.serverboundByVersion.put(version, serverbound);
      this.clientboundByVersion.put(version, clientbound);
    }
    return this;
  }

  @Override
  public PacketRegistryMap lookup(PacketDirection direction, ProtocolVersion version) {
    if (direction == PacketDirection.SERVERBOUND) {
      return this.serverboundByVersion.getOrDefault(version, EmptyPacketRegistryMap.INSTANCE);
    } else if (direction == PacketDirection.CLIENTBOUND) {
      return this.clientboundByVersion.getOrDefault(version, EmptyPacketRegistryMap.INSTANCE);
    } else {
      throw new NullPointerException("direction");
    }
  }
}
