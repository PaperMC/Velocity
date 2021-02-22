package com.velocitypowered.proxy.network.registry.protocol;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;

public interface ProtocolRegistry {
  PacketRegistryMap lookup(PacketDirection direction, ProtocolVersion version);
}
