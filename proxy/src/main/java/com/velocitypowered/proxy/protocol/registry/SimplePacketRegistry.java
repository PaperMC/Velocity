/*
 * Copyright (C) 2025 Velocity Contributors
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

package com.velocitypowered.proxy.protocol.registry;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.PacketCodec;
import com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Extremely simple packet registry implementation that assumes every protocol version is
 * the same.
 */
public class SimplePacketRegistry implements PacketRegistry, ProtocolToPacketRegistry {
  private final IntObjectMap<PacketCodec<? extends MinecraftPacket>> packetIdToCodec =
      new IntObjectHashMap<>(16, 0.5f);
  private final Map<Class<? extends MinecraftPacket>, PacketCodec<? extends MinecraftPacket>> packetClassToCodec =
      new HashMap<>(16, 0.5f);
  private final Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId =
      new Object2IntOpenHashMap<>(16, 0.5f);
  private final Direction direction;

  public SimplePacketRegistry(Direction direction) {
    this.direction = direction;
    this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
  }

  /**
   * Registers a packet for this protocol version.
   *
   * @param id the packet ID
   * @param packet the packet class
   * @param codec the packet codec
   */
  public void register(int id, Class<? extends MinecraftPacket> packet, PacketCodec<? extends MinecraftPacket> codec) {
    this.packetIdToCodec.put(id, codec);
    this.packetClassToCodec.put(packet, codec);
    this.packetClassToId.put(packet, id);
  }

  @Override
  public @Nullable PacketCodec<? extends MinecraftPacket> getCodec(int id) {
    return this.packetIdToCodec.get(id);
  }

  @Override
  public @Nullable <T extends MinecraftPacket> PacketCodec<T> getCodec(Class<T> packetClass) {
    return (PacketCodec<T>) this.packetClassToCodec.get(packetClass);
  }

  @Override
  public int getPacketId(MinecraftPacket packet) {
    final int id = this.packetClassToId.getInt(packet.getClass());
    if (id == Integer.MIN_VALUE) {
      throw new IllegalArgumentException(String.format(
          "Unable to find id for packet of type %s in %s phase %s",
          packet.getClass().getName(), direction, this
      ));
    }
    return id;
  }

  @Override
  public boolean canDecodePacket(MinecraftPacket packet) {
    return packetClassToId.containsKey(packet.getClass());
  }

  @Override
  public PacketRegistry forVersion(ProtocolVersion version) {
    return this;
  }
}
