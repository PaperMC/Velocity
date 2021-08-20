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
import com.velocitypowered.proxy.util.MathUtil;
import com.velocitypowered.proxy.util.hash.PerfectHash;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides a packet registry map that is "dense". Lookups are very fast due to the use of
 * perfect hashing.
 */
public class DensePacketRegistryMap implements PacketRegistryMap {

  private final PacketReader<?>[] readersById;
  private final PacketWriter[] writersBuckets;
  private final Class<?>[] classesBuckets;
  private final int[] idsByClass;
  private final int key;

  public DensePacketRegistryMap(Int2ObjectMap<PacketMapping<?>> mappings) {
    int size = mappings.keySet().stream().mapToInt(x -> x).max().orElse(0) + 1;
    int hashSize = MathUtil.nextHighestPowerOfTwo(size);
    int[] classHashCodes = mappings.values().stream().map(m -> m.packetClass)
        .mapToInt(Object::hashCode).toArray();

    this.key = PerfectHash.findPerfectHashKey(classHashCodes, hashSize);

    this.readersById = new PacketReader[size];
    this.writersBuckets = new PacketWriter[hashSize];
    this.classesBuckets = new Class[hashSize];
    this.idsByClass = new int[hashSize];

    for (PacketMapping<?> value : mappings.values()) {
      final int hashIdx = bucket(value.packetClass);

      this.readersById[value.id] = value.reader;
      this.writersBuckets[hashIdx] = value.writer;
      this.classesBuckets[hashIdx] = value.packetClass;
      this.idsByClass[hashIdx] = value.id;
    }
  }

  private int bucket(final Object o) {
    return PerfectHash.hash(o.hashCode(), this.key, this.classesBuckets.length);
  }

  @Override
  public @Nullable PacketReader<? extends Packet> lookupReader(final int id,
      ProtocolVersion version) {
    if (id < 0 || id >= this.readersById.length) {
      return null;
    }

    return this.readersById[id];
  }

  @Override
  public <P extends Packet> void writePacket(P packet, ByteBuf buf, ProtocolVersion version) {
    int bucket = this.bucket(packet.getClass());
    if (this.classesBuckets[bucket] == packet.getClass()) {
      ProtocolUtils.writeVarInt(buf, this.idsByClass[bucket]);
      this.writersBuckets[bucket].write(buf, packet, version);
    } else {
      throw new IllegalArgumentException(String.format(
          "Unable to find id for packet of type %s in protocol %s",
          packet.getClass().getName(), version
      ));
    }

  }

  @Override
  public @Nullable Class<? extends Packet> lookupPacket(int id) {
    for (int bucket = 0; bucket < this.idsByClass.length; bucket++) {
      if (this.idsByClass[bucket] == id) {
        return (Class<? extends Packet>) this.classesBuckets[bucket];
      }
    }
    return null;
  }
}
