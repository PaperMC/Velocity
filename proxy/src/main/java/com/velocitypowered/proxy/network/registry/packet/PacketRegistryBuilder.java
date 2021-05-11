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

import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PacketRegistryBuilder {

  private final Int2ObjectMap<PacketMapping<?>> mappings;
  private boolean dense = false;

  public PacketRegistryBuilder() {
    this.mappings = new Int2ObjectOpenHashMap<>();
  }

  public PacketRegistryBuilder(Int2ObjectMap<PacketMapping<?>> mappings, boolean dense) {
    this.mappings = new Int2ObjectOpenHashMap<>(mappings);
    this.dense = dense;
  }

  public <P extends Packet> PacketRegistryBuilder register(int id, Class<P> packetClass,
      PacketWriter<P> writer) {
    mappings.put(id, new PacketMapping(id, packetClass, writer, null));
    return this;
  }

  public <P extends Packet> PacketRegistryBuilder register(int id, Class<P> packetClass,
      PacketReader<P> reader, PacketWriter<P> writer) {
    mappings.put(id, new PacketMapping(id, packetClass, writer, reader));
    return this;
  }

  public PacketRegistryBuilder dense() {
    this.dense = true;
    return this;
  }

  public PacketRegistryBuilder copy() {
    return new PacketRegistryBuilder(this.mappings, this.dense);
  }

  public PacketRegistryMap build() {
    if (this.dense) {
      return new DensePacketRegistryMap(mappings);
    } else {
      return new RegularPacketRegistryMap(mappings);
    }
  }

  static final class PacketMapping<P extends Packet> {

    int id;
    final Class<P> packetClass;
    final PacketWriter<P> writer;
    final @Nullable PacketReader<P> reader;

    PacketMapping(int id, Class<P> packetClass,
        PacketWriter<P> writer,
        @Nullable PacketReader<P> reader) {
      this.id = id;
      this.packetClass = packetClass;
      this.writer = writer;
      this.reader = reader;
    }
  }
}
