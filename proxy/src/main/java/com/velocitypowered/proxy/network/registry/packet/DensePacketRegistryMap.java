package com.velocitypowered.proxy.network.registry.packet;

import com.velocitypowered.api.network.ProtocolVersion;
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
  private final Class<?>[] classesById;

  public DensePacketRegistryMap(Int2ObjectMap<PacketMapping<?>> mappings) {
    int size = mappings.keySet().stream().mapToInt(x -> x).max().orElse(0) + 1;

    this.readersById = new PacketReader[size];
    this.writersByClass = new PacketWriter[size * 2];
    this.classesById = new Class[size * 2];

    for (PacketMapping<?> value : mappings.values()) {
      this.readersById[value.id] = value.reader;
      this.place(value.packetClass, value.writer);
    }
  }

  private void place(Class<?> key, PacketWriter<?> value) {
    int bucket = findEmpty(key);
    this.writersByClass[bucket] = value;
    this.classesById[bucket] = key;
  }

  private int findEmpty(Class<?> key) {
    int start = key.hashCode() % this.classesById.length;
    int index = start;

    for (;;) {
      if (this.classesById[index] == null || this.classesById[index].equals(key)) {
        // It's available, so no chance that this value exists anywhere in the map.
        return index;
      }

      if ((index = (index + 1) % this.classesById.length) == start) {
        return -1;
      }
    }
  }

  private int index(Class<?> key) {
    int start = key.hashCode() % this.classesById.length;
    int index = start;

    for (;;) {
      if (this.classesById[index] == null) {
        // It's available, so no chance that this value exists anywhere in the map.
        return -1;
      }
      if (key.equals(this.classesById[index])) {
        return index;
      }

      // Conflict, keep probing ...
      if ((index = (index + 1) % this.classesById.length) == start) {
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
    int id = this.index(packet.getClass());
    if (id != -1) {
      this.writersByClass[id].write(buf, packet, version);
    } else {
      throw new IllegalArgumentException(String.format(
          "Unable to find id for packet of type %s in protocol %s",
          packet.getClass().getName(), version
      ));
    }

  }
}
