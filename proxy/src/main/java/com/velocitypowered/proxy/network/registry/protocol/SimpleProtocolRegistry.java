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
