package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityChannelRegistrar implements ChannelRegistrar {

  private final Map<String, ChannelIdentifier> identifierMap = new ConcurrentHashMap<>();

  @Override
  public void register(ChannelIdentifier... identifiers) {
    for (ChannelIdentifier identifier : identifiers) {
      Preconditions.checkArgument(identifier instanceof LegacyChannelIdentifier
              || identifier instanceof MinecraftChannelIdentifier,
          "identifier is unknown");
    }

    for (ChannelIdentifier identifier : identifiers) {
      identifierMap.put(identifier.getId(), identifier);
    }
  }

  @Override
  public void unregister(ChannelIdentifier... identifiers) {
    for (ChannelIdentifier identifier : identifiers) {
      Preconditions.checkArgument(identifier instanceof LegacyChannelIdentifier
              || identifier instanceof MinecraftChannelIdentifier,
          "identifier is unknown");
    }

    for (ChannelIdentifier identifier : identifiers) {
      identifierMap.remove(identifier.getId());
    }
  }

  public Collection<String> getIdsForLegacyConnections() {
    return ImmutableList.copyOf(identifierMap.keySet());
  }

  /**
   * Returns all channel IDs (as strings) for use with Minecraft 1.13 and above.
   *
   * @return the channel IDs for Minecraft 1.13 and above
   */
  public Collection<String> getModernChannelIds() {
    Collection<String> ids = new ArrayList<>();
    for (ChannelIdentifier value : identifierMap.values()) {
      if (value instanceof MinecraftChannelIdentifier) {
        ids.add(value.getId());
      }
    }
    return ids;
  }

  public boolean registered(String id) {
    return identifierMap.containsKey(id);
  }

  public @Nullable ChannelIdentifier getFromId(String id) {
    return identifierMap.get(id);
  }

  /**
   * Returns all the channel names to register depending on the Minecraft protocol version.
   * @param protocolVersion the protocol version in use
   * @return the list of channels to register
   */
  public Collection<String> getChannelsForProtocol(ProtocolVersion protocolVersion) {
    if (protocolVersion.compareTo(ProtocolVersion.MINECRAFT_1_13) >= 0) {
      return getModernChannelIds();
    }
    return getIdsForLegacyConnections();
  }
}
