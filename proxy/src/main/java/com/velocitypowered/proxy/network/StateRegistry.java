package com.velocitypowered.proxy.network;

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
import com.velocitypowered.proxy.network.packet.Packet;
import com.velocitypowered.proxy.network.packet.PacketDirection;
import com.velocitypowered.proxy.network.packet.PacketReader;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundAvailableCommandsPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundBossBarPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundChatPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundDisconnectPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundEncryptionRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundHeaderAndFooterPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundJoinGamePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundLoginPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPlayerListItemPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundResourcePackRequestPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundRespawnPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundServerLoginSuccessPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundSetCompressionPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundStatusResponsePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTabCompleteResponsePacket;
import com.velocitypowered.proxy.network.packet.clientbound.ClientboundTitlePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundChatPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundClientSettingsPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundEncryptionResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundHandshakePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundKeepAlivePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundLoginPluginResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundPluginMessagePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundResourcePackResponsePacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundServerLoginPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusPingPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundStatusRequestPacket;
import com.velocitypowered.proxy.network.packet.serverbound.ServerboundTabCompleteRequestPacket;
import io.netty.buffer.ByteBuf;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum StateRegistry {

  HANDSHAKE(true) {
    {
      serverbound.register(
          ServerboundHandshakePacket.class,
          ServerboundHandshakePacket.DECODER,
          map(0x00, MINECRAFT_1_7_2, false)
      );
    }
  },
  STATUS(true) {
    {
      serverbound.register(
          ServerboundStatusRequestPacket.class,
          ServerboundStatusRequestPacket.DECODER,
          map(0x00, MINECRAFT_1_7_2, false)
      );
      serverbound.register(
          ServerboundStatusPingPacket.class,
          ServerboundStatusPingPacket.DECODER,
          map(0x01, MINECRAFT_1_7_2, false)
      );

      clientbound.register(
          ClientboundStatusResponsePacket.class,
          ClientboundStatusResponsePacket.DECODER,
          map(0x00, MINECRAFT_1_7_2, false)
      );
      clientbound.register(
          ClientboundStatusPingPacket.class,
          ClientboundStatusPingPacket.DECODER,
          map(0x01, MINECRAFT_1_7_2, false)
      );
    }
  },
  PLAY(false) {
    {
      serverbound.register(
          ServerboundTabCompleteRequestPacket.class,
          ServerboundTabCompleteRequestPacket.DECODER,
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
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x02, MINECRAFT_1_9, false),
          map(0x03, MINECRAFT_1_12, false),
          map(0x02, MINECRAFT_1_12_1, false),
          map(0x03, MINECRAFT_1_14, false)
      );
      serverbound.register(
          ServerboundClientSettingsPacket.class,
          ServerboundClientSettingsPacket.DECODER,
          map(0x15, MINECRAFT_1_7_2, false),
          map(0x04, MINECRAFT_1_9, false),
          map(0x05, MINECRAFT_1_12, false),
          map(0x04, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_14, false)
      );
      serverbound.register(
          ServerboundPluginMessagePacket.class,
          ServerboundPluginMessagePacket.DECODER,
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
          map(0x0C, MINECRAFT_1_9, false),
          map(0x0D, MINECRAFT_1_15, false),
          map(0x0C, MINECRAFT_1_16, false)
      );
      clientbound.register(
          ClientboundChatPacket.class,
          ClientboundChatPacket.DECODER,
          map(0x02, MINECRAFT_1_7_2, true),
          map(0x0F, MINECRAFT_1_9, true),
          map(0x0E, MINECRAFT_1_13, true),
          map(0x0F, MINECRAFT_1_15, true),
          map(0x0E, MINECRAFT_1_16, true)
      );
      clientbound.register(
          ClientboundTabCompleteResponsePacket.class,
          ClientboundTabCompleteResponsePacket.DECODER,
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
          map(0x11, MINECRAFT_1_13, false),
          map(0x12, MINECRAFT_1_15, false),
          map(0x11, MINECRAFT_1_16, false),
          map(0x10, MINECRAFT_1_16_2, false)
      );
      clientbound.register(
          ClientboundPluginMessagePacket.class,
          ClientboundPluginMessagePacket.DECODER,
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
  },
  LOGIN(true) {
    {
      serverbound.register(
          ServerboundServerLoginPacket.class,
          ServerboundServerLoginPacket.DECODER,
          map(0x00, MINECRAFT_1_7_2, false)
      );
      serverbound.register(
          ServerboundEncryptionResponsePacket.class,
          ServerboundEncryptionResponsePacket.DECODER,
          map(0x01, MINECRAFT_1_7_2, false)
      );
      serverbound.register(
          ServerboundLoginPluginResponsePacket.class,
          ServerboundLoginPluginResponsePacket.DECODER,
          map(0x02, MINECRAFT_1_13, false)
      );

      clientbound.register(
          ClientboundDisconnectPacket.class,
          ClientboundDisconnectPacket.DECODER,
          map(0x00, MINECRAFT_1_7_2, false)
      );
      clientbound.register(
          ClientboundEncryptionRequestPacket.class,
          ClientboundEncryptionRequestPacket.DECODER,
          map(0x01, MINECRAFT_1_7_2, false)
      );
      clientbound.register(
          ClientboundServerLoginSuccessPacket.class,
          ClientboundServerLoginSuccessPacket.DECODER,
          map(0x02, MINECRAFT_1_7_2, false)
      );
      clientbound.register(
          ClientboundSetCompressionPacket.class,
          ClientboundSetCompressionPacket.DECODER,
          map(0x03, MINECRAFT_1_8, false)
      );
      clientbound.register(
          ClientboundLoginPluginMessagePacket.class,
          ClientboundLoginPluginMessagePacket.DECODER,
          map(0x04, MINECRAFT_1_13, false)
      );
    }
  };

  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;
  public final PacketRegistry clientbound;
  public final PacketRegistry serverbound;

  StateRegistry(boolean useMinimumIfVersionNotFound) {
    this.clientbound = new PacketRegistry(PacketDirection.CLIENTBOUND, useMinimumIfVersionNotFound);
    this.serverbound = new PacketRegistry(PacketDirection.SERVERBOUND, useMinimumIfVersionNotFound);
  }

  public PacketRegistry.ProtocolRegistry getProtocolRegistry(PacketDirection direction,
                                                             ProtocolVersion version) {
    return (direction == PacketDirection.SERVERBOUND ? this.serverbound : this.clientbound)
      .getProtocolRegistry(version);
  }

  public static class PacketRegistry {

    private final PacketDirection direction;
    private final Map<ProtocolVersion, ProtocolRegistry> versions;
    private final boolean useMinimumIfVersionNotFound;

    PacketRegistry(PacketDirection direction) {
      this(direction, true);
    }

    PacketRegistry(PacketDirection direction, boolean useMinimumIfVersionNotFound) {
      this.direction = direction;
      this.useMinimumIfVersionNotFound = useMinimumIfVersionNotFound;

      Map<ProtocolVersion, ProtocolRegistry> mutableVersions = new EnumMap<>(ProtocolVersion.class);
      for (ProtocolVersion version : ProtocolVersion.values()) {
        if (!version.isLegacy() && !version.isUnknown()) {
          mutableVersions.put(version, new ProtocolRegistry(version));
        }
      }

      this.versions = Collections.unmodifiableMap(mutableVersions);
    }

    ProtocolRegistry getProtocolRegistry(final ProtocolVersion version) {
      ProtocolRegistry registry = versions.get(version);
      if (registry == null) {
        if (useMinimumIfVersionNotFound) {
          return getProtocolRegistry(MINIMUM_VERSION);
        }
        throw new IllegalArgumentException("Could not find data for protocol version " + version);
      }
      return registry;
    }

    <P extends Packet> void register(Class<P> clazz, PacketReader<P> decoder,
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
        }
      }
    }

    public class ProtocolRegistry {

      public final ProtocolVersion version;
      final IntObjectMap<PacketReader<? extends Packet>> packetIdToReader =
          new IntObjectHashMap<>(16, 0.5f);
      final Object2IntMap<Class<? extends Packet>> packetClassToId =
          new Object2IntOpenHashMap<>(16, 0.5f);

      ProtocolRegistry(final ProtocolVersion version) {
        this.version = version;
        this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
      }

      /**
       * Attempts to create a packet from the specified {@code id}.
       *
       * @param id the packet ID
       * @param buf the bytebuf
       * @param direction the packet direction
       * @param version the protocol version
       * @return the packet instance, or {@code null} if the ID is not registered
       */
      public @Nullable Packet readPacket(final int id, ByteBuf buf, PacketDirection direction,
                                         ProtocolVersion version) {
        final PacketReader<? extends Packet> decoder = this.packetIdToReader.get(id);
        if (decoder == null) {
          return null;
        }
        return decoder.read(buf, direction, version);
      }

      /**
       * Attempts to look up the packet ID for an {@code packet}.
       *
       * @param packet the packet to look up
       * @return the packet ID
       * @throws IllegalArgumentException if the packet ID is not found
       */
      public int getPacketId(final Packet packet) {
        final int id = this.packetClassToId.getInt(packet.getClass());
        if (id == Integer.MIN_VALUE) {
          throw new IllegalArgumentException(String.format(
              "Unable to find id for packet of type %s in %s protocol %s",
              packet.getClass().getName(), PacketRegistry.this.direction, this.version
          ));
        }
        return id;
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
