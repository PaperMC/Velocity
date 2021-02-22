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

  public PacketRegistryBuilder(
      Int2ObjectMap<PacketMapping<?>> mappings, boolean dense) {
    this.mappings = new Int2ObjectOpenHashMap<>(mappings);
    this.dense = dense;
  }

  public <P extends Packet> PacketRegistryBuilder register(int id, Class<P> packetClass,
      PacketWriter<P> writer) {
    mappings.put(id, new PacketMapping(id, packetClass, writer, null));
    return this;
  }

  public <P extends Packet> PacketRegistryBuilder register(int id, Class<P> packetClass,
      PacketWriter<P> writer, PacketReader<P> reader) {
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
