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

package com.velocitypowered.proxy.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.messages.MinecraftPluginChannelId;
import com.velocitypowered.api.proxy.messages.PairedPluginChannelId;
import com.velocitypowered.api.proxy.messages.PluginChannelId;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityChannelRegistrar implements ChannelRegistrar {

  private final Map<String, PluginChannelId> byLegacyId = new ConcurrentHashMap<>();
  private final Map<String, PluginChannelId> byKey = new ConcurrentHashMap<>();

  @Override
  public void register(PluginChannelId... identifiers) {
    for (PluginChannelId identifier : identifiers) {
      Preconditions.checkArgument(identifier instanceof PairedPluginChannelId
          || identifier instanceof MinecraftPluginChannelId, "identifier is unknown");
    }

    for (PluginChannelId identifier : identifiers) {
      if (identifier instanceof MinecraftPluginChannelId) {
        MinecraftPluginChannelId modern = (MinecraftPluginChannelId) identifier;
        byLegacyId.put(modern.key().asString(), identifier);
        byKey.put(modern.key().asString(), identifier);
      } else {
        PairedPluginChannelId paired = (PairedPluginChannelId) identifier;
        byLegacyId.put(paired.legacyChannel(), identifier);
        byKey.put(paired.modernChannelKey().asString(), identifier);
      }
    }
  }

  @Override
  public void unregister(PluginChannelId... identifiers) {
    for (PluginChannelId identifier : identifiers) {
      Preconditions.checkArgument(identifier instanceof PairedPluginChannelId
              || identifier instanceof MinecraftPluginChannelId,
          "identifier is unknown");
    }

    for (PluginChannelId identifier : identifiers) {
      if (identifier instanceof MinecraftPluginChannelId) {
        MinecraftPluginChannelId modern = (MinecraftPluginChannelId) identifier;
        byKey.remove(modern.key().asString(), identifier);
      } else {
        PairedPluginChannelId paired = (PairedPluginChannelId) identifier;
        byLegacyId.remove(paired.legacyChannel(), identifier);
        byKey.remove(paired.modernChannelKey().asString(), identifier);
      }
    }
  }

  /**
   * Returns all legacy channel IDs.
   *
   * @return all legacy channel IDs
   */
  public Collection<String> getLegacyChannelIds() {
    return ImmutableSet.copyOf(this.byLegacyId.keySet());
  }

  /**
   * Returns all channel IDs (as strings) for use with Minecraft 1.13 and above.
   *
   * @return the channel IDs for Minecraft 1.13 and above
   */
  public Collection<String> getModernChannelIds() {
    return ImmutableSet.copyOf(this.byKey.keySet());
  }

  public @Nullable PluginChannelId getFromId(String id) {
    if (id.indexOf(':') >= 0) {
      return byKey.get(id);
    }
    return byLegacyId.get(id);
  }

  /**
   * Returns all the channel names to register depending on the Minecraft protocol version.
   * @param protocolVersion the protocol version in use
   * @return the list of channels to register
   */
  public Collection<String> getChannelsForProtocol(ProtocolVersion protocolVersion) {
    if (protocolVersion.gte(ProtocolVersion.MINECRAFT_1_13)) {
      return getModernChannelIds();
    }
    return getLegacyChannelIds();
  }
}
