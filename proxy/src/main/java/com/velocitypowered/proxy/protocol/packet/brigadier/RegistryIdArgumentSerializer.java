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

package com.velocitypowered.proxy.protocol.packet.brigadier;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import io.netty.buffer.ByteBuf;

/**
 * The {@code RegistryIdArgumentSerializer} handles serialization and deserialization
 * of integer-based registry ID arguments.
 *
 * <p>This serializer is used for command arguments that refer to elements in Minecraft
 * registries (e.g., items, entities, dimensions) by their numerical registry ID.</p>
 *
 * <p>Values are encoded as variable-length integers using {@link ProtocolUtils}
 * for compact transmission.</p>
 */
public class RegistryIdArgumentSerializer implements ArgumentPropertySerializer<Integer> {

  static final RegistryIdArgumentSerializer REGISTRY_ID = new RegistryIdArgumentSerializer();

  @Override
  public Integer deserialize(ByteBuf buf, ProtocolVersion protocolVersion) {
    return ProtocolUtils.readVarInt(buf);
  }

  @Override
  public void serialize(Integer object, ByteBuf buf, ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, object);
  }
}
