package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.proxy.protocol.util.PluginMessageUtil;
import java.util.Collection;
import java.util.HashSet;
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
      if (identifier instanceof MinecraftChannelIdentifier) {
        identifierMap.put(identifier.getId(), identifier);
      } else {
        String rewritten = PluginMessageUtil.transformLegacyToModernChannel(identifier.getId());
        identifierMap.put(identifier.getId(), identifier);
        identifierMap.put(rewritten, identifier);
      }
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
      if (identifier instanceof MinecraftChannelIdentifier) {
        identifierMap.remove(identifier.getId());
      } else {
        String rewritten = PluginMessageUtil.transformLegacyToModernChannel(identifier.getId());
        identifierMap.remove(identifier.getId());
        identifierMap.remove(rewritten);
      }
    }
  }

  /**
   * Returns all legacy channel IDs.
   *
   * @return all legacy channel IDs
   */
  public Collection<String> getLegacyChannelIds() {
    Collection<String> ids = new HashSet<>();
    for (ChannelIdentifier value : identifierMap.values()) {
      ids.add(value.getId());
    }
    return ids;
  }

  /**
   * Returns all channel IDs (as strings) for use with Minecraft 1.13 and above.
   *
   * @return the channel IDs for Minecraft 1.13 and above
   */
  public Collection<String> getModernChannelIds() {
    Collection<String> ids = new HashSet<>();
    for (ChannelIdentifier value : identifierMap.values()) {
      if (value instanceof MinecraftChannelIdentifier) {
        ids.add(value.getId());
      } else {
        ids.add(PluginMessageUtil.transformLegacyToModernChannel(value.getId()));
      }
    }
    return ids;
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
    return getLegacyChannelIds();
  }
}
