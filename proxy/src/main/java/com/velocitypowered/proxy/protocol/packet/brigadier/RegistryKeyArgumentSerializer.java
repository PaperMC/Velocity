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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

public class RegistryKeyArgumentSerializer implements ArgumentPropertySerializer<RegistryKeyArgument> {
  static final RegistryKeyArgumentSerializer REGISTRY = new RegistryKeyArgumentSerializer();

  @Override
  public RegistryKeyArgument deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    return new RegistryKeyArgument(ProtocolUtils.readString(buf));
  }

  @Override
  public void serialize(RegistryKeyArgument object, ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeString(buf, object.getIdentifier());
  }
}
