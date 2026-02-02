/*
 * Copyright (C) 2022-2023 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * Represents a list of {@link RegistryKeyArgument} objects.
 *
 * <p>Used to manage and store multiple registry key arguments.</p>
 */
public final class RegistryKeyArgumentList {

  /**
   * Represents a registry key argument that can either be a resource or a tag.
   */
  public static class ResourceOrTag extends RegistryKeyArgument {

    public ResourceOrTag(String identifier) {
      super(identifier);
    }

    /**
     * Serializer for {@link ResourceOrTag}.
     */
    public static class Serializer implements ArgumentPropertySerializer<ResourceOrTag> {

      static final ResourceOrTag.Serializer REGISTRY = new ResourceOrTag.Serializer();

      @Override
      public ResourceOrTag deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
        return new ResourceOrTag(ProtocolUtils.readString(buf));
      }

      @Override
      public void serialize(ResourceOrTag object, ByteBuf buf, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(buf, object.getIdentifier());
      }
    }
  }

  /**
   * Represents a registry key argument specifically for a resource or tag key.
   */
  public static class ResourceOrTagKey extends RegistryKeyArgument {

    public ResourceOrTagKey(String identifier) {
      super(identifier);
    }

    /**
     * Serializer for {@link ResourceOrTagKey}.
     */
    public static class Serializer implements ArgumentPropertySerializer<ResourceOrTagKey> {

      static final ResourceOrTagKey.Serializer REGISTRY = new ResourceOrTagKey.Serializer();

      @Override
      public ResourceOrTagKey deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
        return new ResourceOrTagKey(ProtocolUtils.readString(buf));
      }

      @Override
      public void serialize(ResourceOrTagKey object, ByteBuf buf, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(buf, object.getIdentifier());
      }
    }
  }

  /**
   * Represents a registry key argument for a resource.
   */
  public static class ResourceSelector extends RegistryKeyArgument {

    public ResourceSelector(String identifier) {
      super(identifier);
    }

    /**
     * Serializer for {@link ResourceSelector}.
     */
    public static class Serializer implements ArgumentPropertySerializer<ResourceSelector> {

      static final ResourceSelector.Serializer REGISTRY = new ResourceSelector.Serializer();

      @Override
      public ResourceSelector deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
        return new ResourceSelector(ProtocolUtils.readString(buf));
      }

      @Override
      public void serialize(ResourceSelector object, ByteBuf buf, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(buf, object.getIdentifier());
      }
    }
  }

  /**
   * Represents a registry key argument for a resource key.
   */
  public static class ResourceKey extends RegistryKeyArgument {

    public ResourceKey(String identifier) {
      super(identifier);
    }

    /**
     * Serializer for {@link ResourceKey}.
     */
    public static class Serializer implements ArgumentPropertySerializer<ResourceKey> {

      static final ResourceKey.Serializer REGISTRY = new ResourceKey.Serializer();

      @Override
      public ResourceKey deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
        return new ResourceKey(ProtocolUtils.readString(buf));
      }

      @Override
      public void serialize(ResourceKey object, ByteBuf buf, ProtocolVersion protocolVersion) {
        ProtocolUtils.writeString(buf, object.getIdentifier());
      }
    }
  }

  RegistryKeyArgumentList() {
  }
}
