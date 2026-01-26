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

package com.velocitypowered.proxy.protocol.packet.config;

import com.google.common.collect.ImmutableMap;
import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.ProtocolUtils;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.buffer.ByteBuf;
import java.util.Map;

/**
 * The {@code TagsUpdatePacket} class represents a packet sent to update the tags
 * used by the Minecraft client. Tags are used in various parts of the game to group
 * blocks, items, entities, and other objects under common categories.
 *
 * <p>This packet is typically sent to clients when they join a server or when
 * the server needs to update the list of tags for the client, ensuring that
 * the client has the most up-to-date tag information.</p>
 */
public class TagsUpdatePacket implements MinecraftPacket {

  private Map<String, Map<String, int[]>> tags;

  public TagsUpdatePacket(Map<String, Map<String, int[]>> tags) {
    this.tags = tags;
  }

  public TagsUpdatePacket() {
    this.tags = Map.of();
  }

  @Override
  public void decode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
    ImmutableMap.Builder<String, Map<String, int[]>> builder = ImmutableMap.builder();
    int size = ProtocolUtils.readVarInt(buf);
    for (int i = 0; i < size; i++) {
      String key = ProtocolUtils.readString(buf);

      int innerSize = ProtocolUtils.readVarInt(buf);
      ImmutableMap.Builder<String, int[]> innerBuilder = ImmutableMap.builder();
      for (int j = 0; j < innerSize; j++) {
        String innerKey = ProtocolUtils.readString(buf);
        int[] innerValue = ProtocolUtils.readVarIntArray(buf);
        innerBuilder.put(innerKey, innerValue);
      }

      builder.put(key, innerBuilder.build());
    }
    tags = builder.build();
  }

  @Override
  public void encode(ByteBuf buf, ProtocolUtils.Direction direction,
                     ProtocolVersion protocolVersion) {
    ProtocolUtils.writeVarInt(buf, tags.size());
    for (Map.Entry<String, Map<String, int[]>> entry : tags.entrySet()) {
      ProtocolUtils.writeString(buf, entry.getKey());
      // Oh, joy
      ProtocolUtils.writeVarInt(buf, entry.getValue().size());
      for (Map.Entry<String, int[]> innerEntry : entry.getValue().entrySet()) {
        // Yea, object oriented programming be damned
        ProtocolUtils.writeString(buf, innerEntry.getKey());
        ProtocolUtils.writeVarIntArray(buf, innerEntry.getValue());
      }
    }
  }

  @Override
  public boolean handle(MinecraftSessionHandler handler) {
    return handler.handle(this);
  }

  @Override
  public int encodeSizeHint(Direction direction, ProtocolVersion version) {
    int size = ProtocolUtils.varIntBytes(tags.size());
    for (Map.Entry<String, Map<String, int[]>> entry : tags.entrySet()) {
      size += ProtocolUtils.stringSizeHint(entry.getKey());
      size += ProtocolUtils.varIntBytes(entry.getValue().size());
      for (Map.Entry<String, int[]> innerEntry : entry.getValue().entrySet()) {
        size += ProtocolUtils.stringSizeHint(innerEntry.getKey());
        size += ProtocolUtils.varIntBytes(innerEntry.getValue().length);
        for (int innerEntryValue : innerEntry.getValue()) {
          size += ProtocolUtils.varIntBytes(innerEntryValue);
        }
      }
    }

    return size;
  }
}
