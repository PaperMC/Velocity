/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

package com.velocitypowered.proxy.connection.registry;

import com.velocitypowered.proxy.connection.player.VelocityResourcePackInfo;
import com.velocitypowered.proxy.protocol.packet.config.RegistrySync;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the registry data that is sent
 * to the client during the config stage.
 */
public class ClientConfigData {

  private final @Nullable VelocityResourcePackInfo resourcePackInfo;
  private final DataTag tag;
  private final RegistrySync registry;
  private final Key[] features;
  private final String brand;

  private ClientConfigData(@Nullable VelocityResourcePackInfo resourcePackInfo, DataTag tag,
                           RegistrySync registry, Key[] features, String brand) {
    this.resourcePackInfo = resourcePackInfo;
    this.tag = tag;
    this.registry = registry;
    this.features = features;
    this.brand = brand;
  }

  public RegistrySync getRegistry() {
    return registry;
  }

  public DataTag getTag() {
    return tag;
  }

  public Key[] getFeatures() {
    return features;
  }

  public @Nullable VelocityResourcePackInfo getResourcePackInfo() {
    return resourcePackInfo;
  }

  public String getBrand() {
    return brand;
  }

  /**
   * Creates a new builder.
   *
   * @return ClientConfigData.Builder
   */
  public static ClientConfigData.Builder builder() {
    return new Builder();
  }

  /**
   * Builder for ClientConfigData.
   */
  public static class Builder {
    private VelocityResourcePackInfo resourcePackInfo;
    private DataTag tag;
    private RegistrySync registry;
    private Key[] features;
    private String brand;

    private Builder() {
    }

    /**
     * Clears the builder.
     */
    public void clear() {
      this.resourcePackInfo = null;
      this.tag = null;
      this.registry = null;
      this.features = null;
      this.brand = null;
    }

    public Builder resourcePack(@Nullable VelocityResourcePackInfo resourcePackInfo) {
      this.resourcePackInfo = resourcePackInfo;
      return this;
    }

    public Builder dataTag(DataTag tag) {
      this.tag = tag;
      return this;
    }

    public Builder registry(RegistrySync registry) {
      this.registry = registry;
      return this;
    }

    public Builder features(Key[] features) {
      this.features = features;
      return this;
    }

    public Builder brand(String brand) {
      this.brand = brand;
      return this;
    }

    public ClientConfigData build() {
      return new ClientConfigData(resourcePackInfo, tag, registry, features, brand);
    }
  }
}
