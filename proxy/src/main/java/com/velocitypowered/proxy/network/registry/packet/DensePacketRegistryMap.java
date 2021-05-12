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

package com.velocitypowered.proxy.network.registry.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryBuilder.PacketMapping;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides a packet registry map that is "dense", ideal for registries that are tightly packed
 * together by ID. Lookups for readers are very fast (O(1)) and for writers uses an embedded
 * open-addressing, probing hash map to conserve memory.
 */
public class DensePacketRegistryMap implements PacketRegistryMap {

  private final PacketReader<?>[] readersById;
  private final PacketWriter[] writersByClass;
  private final Class<?>[] classesByKey;
  private final int[] idsByKey;

  public DensePacketRegistryMap(Int2ObjectMap<PacketMapping<?>> mappings) {
    int size = mappings.keySet().stream().mapToInt(x -> x).max().orElse(0) + 1;

    this.readersById = new PacketReader[size];
    this.writersByClass = new PacketWriter[size * 2];
    this.classesByKey = new Class[size * 2];
    this.idsByKey = new int[size * 2];

    for (PacketMapping<?> value : mappings.values()) {
      this.readersById[value.id] = value.reader;
      this.place(value.id, value.packetClass, value.writer);
    }
  }

  private void place(int packetId, Class<?> key, PacketWriter<?> value) {
    int bucket = findEmpty(key);
    this.writersByClass[bucket] = value;
    this.classesByKey[bucket] = key;
    this.idsByKey[bucket] = packetId;
  }

  private int findEmpty(Class<?> key) {
    int start = key.hashCode() % this.classesByKey.length;
    int index = start;

    for (;;) {
      if (this.classesByKey[index] == null || this.classesByKey[index].equals(key)) {
        // It's available, so no chance that this value exists anywhere in the map.
        return index;
      }

      if ((index = (index + 1) % this.classesByKey.length) == start) {
        return -1;
      }
    }
  }

  private int index(Class<?> key) {
    int start = key.hashCode() % this.classesByKey.length;
    int index = start;

    for (;;) {
      if (this.classesByKey[index] == null) {
        // It's available, so no chance that this value exists anywhere in the map.
        return -1;
      }
      if (key.equals(this.classesByKey[index])) {
        return index;
      }

      // Conflict, keep probing ...
      if ((index = (index + 1) % this.classesByKey.length) == start) {
        return -1;
      }
    }
  }

  @Override
  public @Nullable Packet readPacket(int id, ByteBuf buf, ProtocolVersion version) {
    if (id < 0 || id >= this.readersById.length) {
      return null;
    }

    return this.readersById[id].read(buf, version);
  }

  @Override
  public <P extends Packet> void writePacket(P packet, ByteBuf buf, ProtocolVersion version) {
    int bucket = this.index(packet.getClass());
    if (bucket != -1) {
      ProtocolUtils.writeVarInt(buf, this.idsByKey[bucket]);
      this.writersByClass[bucket].write(buf, packet, version);
    } else {
      throw new IllegalArgumentException(String.format(
          "Unable to find id for packet of type %s in protocol %s",
          packet.getClass().getName(), version
      ));
    }

  }

  @Override
  public @Nullable Class<? extends Packet> lookupPacket(int id) {
    for (int bucket = 0; bucket < this.idsByKey.length; bucket++) {
      if (this.idsByKey[bucket] == id) {
        return (Class<? extends Packet>) this.classesByKey[bucket];
      }
    }
    return null;
  }
}
