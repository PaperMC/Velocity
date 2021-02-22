package com.velocitypowered.proxy.network.registry.packet;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryBuilder.PacketMapping;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * The canonical implementation of the packet registry map.
 */
public class RegularPacketRegistryMap implements PacketRegistryMap {

  private final Int2ObjectMap<PacketReader<?>> readersById;
  private final Int2ObjectMap<PacketWriter<?>> writersById;
  private final Object2IntMap<Class<?>> classesById;

  public RegularPacketRegistryMap(Int2ObjectMap<PacketMapping<?>> mappings) {
    int size = mappings.size();

    this.readersById = new Int2ObjectOpenHashMap<>(size);
    this.writersById = new Int2ObjectOpenHashMap<>(size);
    this.classesById = new Object2IntOpenHashMap<>(size);
    this.classesById.defaultReturnValue(Integer.MIN_VALUE);

    for (PacketMapping<?> value : mappings.values()) {
      if (value.reader != null) {
        this.readersById.put(value.id, value.reader);
      }

      this.writersById.put(value.id, value.writer);
      this.classesById.put(value.packetClass, value.id);
    }
  }

  @Override
  public @Nullable Packet readPacket(int id, ByteBuf buf, ProtocolVersion version) {
    PacketReader<?> reader = this.readersById.get(id);
    if (reader == null) {
      return null;
    }
    return reader.read(buf, version);
  }

  @Override
  public <P extends Packet> void writePacket(P packet, ByteBuf buf, ProtocolVersion version) {
    int packetId = this.classesById.getInt(packet.getClass());
    if (packetId == Integer.MIN_VALUE) {
      throw new IllegalArgumentException(String.format(
          "Unable to find id for packet of type %s in protocol %s",
          packet.getClass().getName(), version
      ));
    }

    PacketWriter writer = this.writersById.get(packetId);
    assert writer != null;
    writer.write(buf, packet, version);

  }
}
