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
 * A collection of the registry-key argument variants used by modern command arguments.
 */
public final class RegistryKeyArgumentList {

  /**
   * A registry-key argument that accepts either a single resource or a tag.
   */
  public static class ResourceOrTag extends RegistryKeyArgument {

    public ResourceOrTag(String identifier) {
      super(identifier);
    }

    /**
     * The argument property serializer for {@link ResourceOrTag} arguments.
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
   * A registry-key argument that accepts either a resource key or a tag key.
   */
  public static class ResourceOrTagKey extends RegistryKeyArgument {

    public ResourceOrTagKey(String identifier) {
      super(identifier);
    }

    /**
     * The argument property serializer for {@link ResourceOrTagKey} arguments.
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
   * A registry-key argument that accepts a resource selector.
   */
  public static class ResourceSelector extends RegistryKeyArgument {

    public ResourceSelector(String identifier) {
      super(identifier);
    }

    /**
     * The argument property serializer for {@link ResourceSelector} arguments.
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
   * A registry-key argument that accepts a single resource key.
   */
  public static class ResourceKey extends RegistryKeyArgument {

    public ResourceKey(String identifier) {
      super(identifier);
    }

    /**
     * The argument property serializer for {@link ResourceKey} arguments.
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
