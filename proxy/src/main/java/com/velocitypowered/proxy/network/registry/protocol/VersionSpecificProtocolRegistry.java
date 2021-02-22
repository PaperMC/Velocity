package com.velocitypowered.proxy.network.registry.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.PacketDirection;
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
      return this.serverboundByVersion.get(version);
    } else if (direction == PacketDirection.CLIENTBOUND) {
      return this.clientboundByVersion.get(version);
    } else {
      throw new NullPointerException("direction");
    }
  }
}
