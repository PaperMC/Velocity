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

package com.velocitypowered.proxy.network.registry.state;

import static com.google.common.collect.Iterables.getLast;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_15;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_16_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_7_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINIMUM_VERSION;
import static com.velocitypowered.api.network.ProtocolVersion.SUPPORTED_VERSIONS;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.network.ProtocolUtils;
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.PacketWriter;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundAvailableCommandsPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundBossBarPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundChatPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundHeaderAndFooterPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundJoinGamePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPlayerListItemPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundResourcePackRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundRespawnPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTabCompleteResponsePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTitlePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundChatPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundClientSettingsPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundResourcePackResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundTabCompleteRequestPacket;
import com.velocitypowered.proxy.network.registry.packet.PacketRegistryMap;
import com.velocitypowered.proxy.network.registry.protocol.ProtocolRegistry;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

class PlayPacketRegistry implements ProtocolRegistry {

  static final ProtocolRegistry PLAY = new PlayPacketRegistry();

  private PlayPacketRegistry() {
    this.clientbound = new PacketRegistry(PacketDirection.CLIENTBOUND);
    this.serverbound = new PacketRegistry(PacketDirection.SERVERBOUND);

    {
      serverbound.register(
          ServerboundTabCompleteRequestPacket.class,
          ServerboundTabCompleteRequestPacket.DECODER,
          ServerboundTabCompleteRequestPacket.ENCODER,
          map(0x14, MINECRAFT_1_7_2, false),
          map(0x01, MINECRAFT_1_9, false),
          map(0x02, MINECRAFT_1_12, false),
          map(0x01, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_13, false),
          map(0x06, MINECRAFT_1_14, false)
      );
      serverbound.register(
          ServerboundChatPacket.class,
          ServerboundChatPacket.DECODER,
          ServerboundChatPacket.ENCODER,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x02, MINECRAFT_1_9, false),
          map(0x03, MINECRAFT_1_12, false),
          map(0x02, MINECRAFT_1_12_1, false),
          map(0x03, MINECRAFT_1_14, false)
      );
      serverbound.register(
          ServerboundClientSettingsPacket.class,
          ServerboundClientSettingsPacket.DECODER,
          ServerboundClientSettingsPacket.ENCODER,
          map(0x15, MINECRAFT_1_7_2, false),
          map(0x04, MINECRAFT_1_9, false),
          map(0x05, MINECRAFT_1_12, false),
          map(0x04, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_14, false)
      );
      serverbound.register(
          ServerboundPluginMessagePacket.class,
          ServerboundPluginMessagePacket.DECODER,
          ServerboundPluginMessagePacket.ENCODER,
          map(0x17, MINECRAFT_1_7_2, false),
          map(0x09, MINECRAFT_1_9, false),
          map(0x0A, MINECRAFT_1_12, false),
          map(0x09, MINECRAFT_1_12_1, false),
          map(0x0A, MINECRAFT_1_13, false),
          map(0x0B, MINECRAFT_1_14, false)
      );
      serverbound.register(
          ServerboundKeepAlivePacket.class,
          ServerboundKeepAlivePacket.DECODER,
          ServerboundKeepAlivePacket.ENCODER,
          map(0x00, MINECRAFT_1_7_2, false),
          map(0x0B, MINECRAFT_1_9, false),
          map(0x0C, MINECRAFT_1_12, false),
          map(0x0B, MINECRAFT_1_12_1, false),
          map(0x0E, MINECRAFT_1_13, false),
          map(0x0F, MINECRAFT_1_14, false),
          map(0x10, MINECRAFT_1_16, false)
      );
      serverbound.register(
          ServerboundResourcePackResponsePacket.class,
          ServerboundResourcePackResponsePacket.DECODER,
          ServerboundResourcePackResponsePacket.ENCODER,
          map(0x19, MINECRAFT_1_8, false),
          map(0x16, MINECRAFT_1_9, false),
          map(0x18, MINECRAFT_1_12, false),
          map(0x1D, MINECRAFT_1_13, false),
          map(0x1F, MINECRAFT_1_14, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x21, MINECRAFT_1_16_2, false)
      );

      clientbound.register(
          ClientboundBossBarPacket.class,
          ClientboundBossBarPacket.DECODER,
          ClientboundBossBarPacket.ENCODER,
          map(0x0C, MINECRAFT_1_9, false),
          map(0x0D, MINECRAFT_1_15, false),
          map(0x0C, MINECRAFT_1_16, false)
      );
      clientbound.register(
          ClientboundChatPacket.class,
          ClientboundChatPacket.DECODER,
          ClientboundChatPacket.ENCODER,
          map(0x02, MINECRAFT_1_7_2, true),
          map(0x0F, MINECRAFT_1_9, true),
          map(0x0E, MINECRAFT_1_13, true),
          map(0x0F, MINECRAFT_1_15, true),
          map(0x0E, MINECRAFT_1_16, true)
      );
      clientbound.register(
          ClientboundTabCompleteResponsePacket.class,
          ClientboundTabCompleteResponsePacket.DECODER,
          ClientboundTabCompleteResponsePacket.ENCODER,
          map(0x3A, MINECRAFT_1_7_2, false),
          map(0x0E, MINECRAFT_1_9, false),
          map(0x10, MINECRAFT_1_13, false),
          map(0x11, MINECRAFT_1_15, false),
          map(0x10, MINECRAFT_1_16, false),
          map(0x0F, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundAvailableCommandsPacket.class,
          ClientboundAvailableCommandsPacket.DECODER,
          ClientboundAvailableCommandsPacket.ENCODER,
          map(0x11, MINECRAFT_1_13, false),
          map(0x12, MINECRAFT_1_15, false),
          map(0x11, MINECRAFT_1_16, false),
          map(0x10, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundPluginMessagePacket.class,
          ClientboundPluginMessagePacket.DECODER,
          ClientboundPluginMessagePacket.ENCODER,
          map(0x3F, MINECRAFT_1_7_2, false),
          map(0x18, MINECRAFT_1_9, false),
          map(0x19, MINECRAFT_1_13, false),
          map(0x18, MINECRAFT_1_14, false),
          map(0x19, MINECRAFT_1_15, false),
          map(0x18, MINECRAFT_1_16, false),
          map(0x17, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundDisconnectPacket.class,
          ClientboundDisconnectPacket.DECODER,
          ClientboundDisconnectPacket.ENCODER,
          map(0x40, MINECRAFT_1_7_2, false),
          map(0x1A, MINECRAFT_1_9, false),
          map(0x1B, MINECRAFT_1_13, false),
          map(0x1A, MINECRAFT_1_14, false),
          map(0x1B, MINECRAFT_1_15, false),
          map(0x1A, MINECRAFT_1_16, false),
          map(0x19, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundKeepAlivePacket.class,
          ClientboundKeepAlivePacket.DECODER,
          ClientboundKeepAlivePacket.ENCODER,
          map(0x00, MINECRAFT_1_7_2, false),
          map(0x1F, MINECRAFT_1_9, false),
          map(0x21, MINECRAFT_1_13, false),
          map(0x20, MINECRAFT_1_14, false),
          map(0x21, MINECRAFT_1_15, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x1F, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundJoinGamePacket.class,
          ClientboundJoinGamePacket.DECODER,
          ClientboundJoinGamePacket.ENCODER,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x23, MINECRAFT_1_9, false),
          map(0x25, MINECRAFT_1_13, false),
          map(0x25, MINECRAFT_1_14, false),
          map(0x26, MINECRAFT_1_15, false),
          map(0x25, MINECRAFT_1_16, false),
          map(0x24, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundRespawnPacket.class,
          ClientboundRespawnPacket.DECODER,
          ClientboundRespawnPacket.ENCODER,
          map(0x07, MINECRAFT_1_7_2, true),
          map(0x33, MINECRAFT_1_9, true),
          map(0x34, MINECRAFT_1_12, true),
          map(0x35, MINECRAFT_1_12_1, true),
          map(0x38, MINECRAFT_1_13, true),
          map(0x3A, MINECRAFT_1_14, true),
          map(0x3B, MINECRAFT_1_15, true),
          map(0x3A, MINECRAFT_1_16, true),
          map(0x39, MINECRAFT_1_16_2, true)
      );
      clientbound.register(
          ClientboundResourcePackRequestPacket.class,
          ClientboundResourcePackRequestPacket.DECODER,
          ClientboundResourcePackRequestPacket.ENCODER,
          map(0x48, MINECRAFT_1_8, true),
          map(0x32, MINECRAFT_1_9, true),
          map(0x33, MINECRAFT_1_12, true),
          map(0x34, MINECRAFT_1_12_1, true),
          map(0x37, MINECRAFT_1_13, true),
          map(0x39, MINECRAFT_1_14, true),
          map(0x3A, MINECRAFT_1_15, true),
          map(0x39, MINECRAFT_1_16, true),
          map(0x38, MINECRAFT_1_16_2, true)
      );
      clientbound.register(
          ClientboundHeaderAndFooterPacket.class,
          ClientboundHeaderAndFooterPacket.DECODER,
          ClientboundHeaderAndFooterPacket.ENCODER,
          map(0x47, MINECRAFT_1_8, true),
          map(0x48, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_9_4, true),
          map(0x49, MINECRAFT_1_12, true),
          map(0x4A, MINECRAFT_1_12_1, true),
          map(0x4E, MINECRAFT_1_13, true),
          map(0x53, MINECRAFT_1_14, true),
          map(0x54, MINECRAFT_1_15, true),
          map(0x53, MINECRAFT_1_16, true)
      );
      clientbound.register(
          ClientboundTitlePacket.class,
          ClientboundTitlePacket.DECODER,
          ClientboundTitlePacket.ENCODER,
          map(0x45, MINECRAFT_1_8, true),
          map(0x45, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_12, true),
          map(0x48, MINECRAFT_1_12_1, true),
          map(0x4B, MINECRAFT_1_13, true),
          map(0x4F, MINECRAFT_1_14, true),
          map(0x50, MINECRAFT_1_15, true),
          map(0x4F, MINECRAFT_1_16, true)
      );
      clientbound.register(
          ClientboundPlayerListItemPacket.class,
          ClientboundPlayerListItemPacket.DECODER,
          ClientboundPlayerListItemPacket.ENCODER,
          map(0x38, MINECRAFT_1_7_2, false),
          map(0x2D, MINECRAFT_1_9, false),
          map(0x2E, MINECRAFT_1_12_1, false),
          map(0x30, MINECRAFT_1_13, false),
          map(0x33, MINECRAFT_1_14, false),
          map(0x34, MINECRAFT_1_15, false),
          map(0x33, MINECRAFT_1_16, false),
          map(0x32, MINECRAFT_1_16_2, false)
      );
    }

    serverbound.compact();
    clientbound.compact();
  }

  public final PacketRegistry clientbound;
  public final PacketRegistry serverbound;

  public PacketRegistryMap lookup(PacketDirection direction,
      ProtocolVersion version) {
    return (direction == PacketDirection.SERVERBOUND ? this.serverbound : this.clientbound)
      .getProtocolRegistry(version);
  }

  public static class PacketRegistry {

    private final PacketDirection direction;
    private final Map<ProtocolVersion, ProtocolRegistry> versions;

    PacketRegistry(PacketDirection direction) {
      this.direction = direction;

      Map<ProtocolVersion, ProtocolRegistry> mutableVersions = new EnumMap<>(ProtocolVersion.class);
      for (ProtocolVersion version : ProtocolVersion.values()) {
        if (!version.isLegacy() && !version.isUnknown()) {
          mutableVersions.put(version, new ProtocolRegistry(version));
        }
      }

      this.versions = mutableVersions;
    }

    ProtocolRegistry getProtocolRegistry(final ProtocolVersion version) {
      ProtocolRegistry registry = versions.get(version);
      if (registry == null) {
        throw new IllegalArgumentException("Could not find data for protocol version " + version);
      }
      return registry;
    }

    <P extends Packet> void register(Class<P> clazz, PacketReader<P> decoder, PacketWriter<P> encoder,
                                     PacketMapping... mappings) {
      if (mappings.length == 0) {
        throw new IllegalArgumentException("At least one mapping must be provided.");
      }

      for (int i = 0; i < mappings.length; i++) {
        PacketMapping current = mappings[i];
        PacketMapping next = (i + 1 < mappings.length) ? mappings[i + 1] : current;
        ProtocolVersion from = current.protocolVersion;
        ProtocolVersion to = current == next ? getLast(SUPPORTED_VERSIONS) : next.protocolVersion;

        if (from.compareTo(to) >= 0 && from != getLast(SUPPORTED_VERSIONS)) {
          throw new IllegalArgumentException(String.format(
              "Next mapping version (%s) should be lower then current (%s)", to, from));
        }

        for (ProtocolVersion protocol : EnumSet.range(from, to)) {
          if (protocol == to && next != current) {
            break;
          }
          ProtocolRegistry registry = this.versions.get(protocol);
          if (registry == null) {
            throw new IllegalArgumentException("Unknown protocol version "
                + current.protocolVersion);
          }

          if (registry.packetIdToReader.containsKey(current.id)) {
            throw new IllegalArgumentException("Can not register class " + clazz.getSimpleName()
                + " with id " + current.id + " for " + registry.version
                + " because another packet is already registered");
          }

          if (registry.packetClassToId.containsKey(clazz)) {
            throw new IllegalArgumentException(clazz.getSimpleName()
                + " is already registered for version " + registry.version);
          }

          if (!current.encodeOnly) {
            registry.packetIdToReader.put(current.id, decoder);
          }
          registry.packetClassToId.put(clazz, current.id);
          registry.packetClassToWriter.put(clazz, encoder);
        }
      }
    }

    public void compact() {
      ProtocolRegistry last = this.versions.get(MINIMUM_VERSION);
      for (Entry<ProtocolVersion, ProtocolRegistry> entry : this.versions
          .entrySet()) {
        if (entry.getValue() == last) {
          continue;
        }

        if (entry.getValue().packetClassToId.equals(last.packetClassToId)
            && entry.getValue().packetClassToWriter.equals(last.packetClassToWriter)) {
          entry.setValue(last);
        } else {
          last = entry.getValue();
        }
      }
    }

    public class ProtocolRegistry implements PacketRegistryMap {

      private final ProtocolVersion version;
      final IntObjectMap<PacketReader<? extends Packet>> packetIdToReader =
          new IntObjectHashMap<>(16, 0.5f);
      final Object2IntMap<Class<? extends Packet>> packetClassToId =
          new Object2IntOpenHashMap<>(16, 0.5f);
      final Map<Class<? extends Packet>, PacketWriter<? extends Packet>> packetClassToWriter =
          new HashMap<>(16, 0.5f);

      ProtocolRegistry(final ProtocolVersion version) {
        this.version = version;
        this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
      }

      /**
       * Attempts to create a packet from the specified {@code id}.
       *
       * @param id the packet ID
       * @param buf the bytebuf
       * @return the packet instance, or {@code null} if the ID is not registered
       */
      public @Nullable Packet readPacket(final int id, ByteBuf buf, ProtocolVersion version) {
        final PacketReader<? extends Packet> decoder = this.packetIdToReader.get(id);
        if (decoder == null) {
          return null;
        }
        return decoder.read(buf, version);
      }

      /**
       * Attempts to serialize the specified {@code packet}.
       *
       * @param packet the packet
       * @param buf the bytebuf
       */
      public <P extends Packet> void writePacket(P packet, ByteBuf buf, ProtocolVersion version) {
        final int id = this.packetClassToId.getInt(packet.getClass());
        if (id == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(String.format(
              "Unable to find id for packet of type %s in %s protocol %s",
              packet.getClass().getName(), PacketRegistry.this.direction, this.version
          ));
        }

        @SuppressWarnings("rawtypes")
        // Safe because all registering actions are type-safe.
        final PacketWriter encoder = this.packetClassToWriter.get(packet.getClass());

        assert encoder != null : "Couldn't look up encoder - shouldn't happen!";

        ProtocolUtils.writeVarInt(buf, id);
        //noinspection unchecked
        encoder.write(buf, packet, version);
      }
    }
  }

  public static final class PacketMapping {

    final int id;
    final ProtocolVersion protocolVersion;
    final boolean encodeOnly;

    PacketMapping(int id, ProtocolVersion protocolVersion, boolean packetDecoding) {
      this.id = id;
      this.protocolVersion = protocolVersion;
      this.encodeOnly = packetDecoding;
    }

    @Override
    public String toString() {
      return "PacketMapping{"
          + "id=" + id
          + ", protocolVersion=" + protocolVersion
          + ", encodeOnly=" + encodeOnly
          + '}';
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PacketMapping that = (PacketMapping) o;
      return id == that.id
          && protocolVersion == that.protocolVersion
          && encodeOnly == that.encodeOnly;
    }

    @Override
    public int hashCode() {
      return Objects.hash(id, protocolVersion, encodeOnly);
    }
  }

  /**
   * Creates a PacketMapping using the provided arguments.
   *
   * @param id         Packet Id
   * @param version    Protocol version
   * @param encodeOnly When true packet decoding will be disabled
   * @return PacketMapping with the provided arguments
   */
  private static PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
    return new PacketMapping(id, version, encodeOnly);
  }

}
