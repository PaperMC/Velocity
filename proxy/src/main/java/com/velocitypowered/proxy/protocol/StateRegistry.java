package com.velocitypowered.proxy.protocol;

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
import com.velocitypowered.proxy.protocol.packet.AvailableCommandsPacket;
import com.velocitypowered.proxy.protocol.packet.BossBarPacket;
import com.velocitypowered.proxy.protocol.packet.ChatPacket;
import com.velocitypowered.proxy.protocol.packet.ClientSettingsPacket;
import com.velocitypowered.proxy.protocol.packet.DisconnectPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequestPacket;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket;
import com.velocitypowered.proxy.protocol.packet.HandshakePacket;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooterPacket;
import com.velocitypowered.proxy.protocol.packet.JoinGamePacket;
import com.velocitypowered.proxy.protocol.packet.KeepAlivePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponsePacket;
import com.velocitypowered.proxy.protocol.packet.PlayerListItemPacket;
import com.velocitypowered.proxy.protocol.packet.PluginMessagePacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequestPacket;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponsePacket;
import com.velocitypowered.proxy.protocol.packet.RespawnPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccessPacket;
import com.velocitypowered.proxy.protocol.packet.SetCompressionPacket;
import com.velocitypowered.proxy.protocol.packet.StatusPingPacket;
import com.velocitypowered.proxy.protocol.packet.StatusRequestPacket;
import com.velocitypowered.proxy.protocol.packet.StatusResponsePacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequestPacket;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponsePacket;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public enum StateRegistry {

  HANDSHAKE {
    {
      serverbound.register(HandshakePacket.class, HandshakePacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
    }
  },
  STATUS {
    {
      serverbound.register(StatusRequestPacket.class, () -> StatusRequestPacket.INSTANCE,
          map(0x00, MINECRAFT_1_7_2, false));
      serverbound.register(StatusPingPacket.class, StatusPingPacket::new,
          map(0x01, MINECRAFT_1_7_2, false));

      clientbound.register(StatusResponsePacket.class, StatusResponsePacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
      clientbound.register(StatusPingPacket.class, StatusPingPacket::new,
          map(0x01, MINECRAFT_1_7_2, false));
    }
  },
  PLAY {
    {
      serverbound.fallback = false;
      clientbound.fallback = false;

      serverbound.register(TabCompleteRequestPacket.class, TabCompleteRequestPacket::new,
          map(0x14, MINECRAFT_1_7_2, false),
          map(0x01, MINECRAFT_1_9, false),
          map(0x02, MINECRAFT_1_12, false),
          map(0x01, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_13, false),
          map(0x06, MINECRAFT_1_14, false));
      serverbound.register(ChatPacket.class, ChatPacket::new,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x02, MINECRAFT_1_9, false),
          map(0x03, MINECRAFT_1_12, false),
          map(0x02, MINECRAFT_1_12_1, false),
          map(0x03, MINECRAFT_1_14, false));
      serverbound.register(ClientSettingsPacket.class, ClientSettingsPacket::new,
          map(0x15, MINECRAFT_1_7_2, false),
          map(0x04, MINECRAFT_1_9, false),
          map(0x05, MINECRAFT_1_12, false),
          map(0x04, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_14, false));
      serverbound.register(PluginMessagePacket.class, PluginMessagePacket::new,
          map(0x17, MINECRAFT_1_7_2, false),
          map(0x09, MINECRAFT_1_9, false),
          map(0x0A, MINECRAFT_1_12, false),
          map(0x09, MINECRAFT_1_12_1, false),
          map(0x0A, MINECRAFT_1_13, false),
          map(0x0B, MINECRAFT_1_14, false));
      serverbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
          map(0x00, MINECRAFT_1_7_2, false),
          map(0x0B, MINECRAFT_1_9, false),
          map(0x0C, MINECRAFT_1_12, false),
          map(0x0B, MINECRAFT_1_12_1, false),
          map(0x0E, MINECRAFT_1_13, false),
          map(0x0F, MINECRAFT_1_14, false),
          map(0x10, MINECRAFT_1_16, false));
      serverbound.register(ResourcePackResponsePacket.class, ResourcePackResponsePacket::new,
          map(0x19, MINECRAFT_1_8, false),
          map(0x16, MINECRAFT_1_9, false),
          map(0x18, MINECRAFT_1_12, false),
          map(0x1D, MINECRAFT_1_13, false),
          map(0x1F, MINECRAFT_1_14, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x21, MINECRAFT_1_16_2, false));

      clientbound.register(BossBarPacket.class, BossBarPacket::new,
          map(0x0C, MINECRAFT_1_9, false),
          map(0x0D, MINECRAFT_1_15, false),
          map(0x0C, MINECRAFT_1_16, false));
      clientbound.register(ChatPacket.class, ChatPacket::new,
          map(0x02, MINECRAFT_1_7_2, true),
          map(0x0F, MINECRAFT_1_9, true),
          map(0x0E, MINECRAFT_1_13, true),
          map(0x0F, MINECRAFT_1_15, true),
          map(0x0E, MINECRAFT_1_16, true));
      clientbound.register(TabCompleteResponsePacket.class, TabCompleteResponsePacket::new,
          map(0x3A, MINECRAFT_1_7_2, false),
          map(0x0E, MINECRAFT_1_9, false),
          map(0x10, MINECRAFT_1_13, false),
          map(0x11, MINECRAFT_1_15, false),
          map(0x10, MINECRAFT_1_16, false),
          map(0x0F, MINECRAFT_1_16_2, false));
      clientbound.register(AvailableCommandsPacket.class, AvailableCommandsPacket::new,
          map(0x11, MINECRAFT_1_13, false),
          map(0x12, MINECRAFT_1_15, false),
          map(0x11, MINECRAFT_1_16, false),
          map(0x10, MINECRAFT_1_16_2, false));
      clientbound.register(PluginMessagePacket.class, PluginMessagePacket::new,
          map(0x3F, MINECRAFT_1_7_2, false),
          map(0x18, MINECRAFT_1_9, false),
          map(0x19, MINECRAFT_1_13, false),
          map(0x18, MINECRAFT_1_14, false),
          map(0x19, MINECRAFT_1_15, false),
          map(0x18, MINECRAFT_1_16, false),
          map(0x17, MINECRAFT_1_16_2, false));
      clientbound.register(DisconnectPacket.class, DisconnectPacket::new,
          map(0x40, MINECRAFT_1_7_2, false),
          map(0x1A, MINECRAFT_1_9, false),
          map(0x1B, MINECRAFT_1_13, false),
          map(0x1A, MINECRAFT_1_14, false),
          map(0x1B, MINECRAFT_1_15, false),
          map(0x1A, MINECRAFT_1_16, false),
          map(0x19, MINECRAFT_1_16_2, false));
      clientbound.register(KeepAlivePacket.class, KeepAlivePacket::new,
          map(0x00, MINECRAFT_1_7_2, false),
          map(0x1F, MINECRAFT_1_9, false),
          map(0x21, MINECRAFT_1_13, false),
          map(0x20, MINECRAFT_1_14, false),
          map(0x21, MINECRAFT_1_15, false),
          map(0x20, MINECRAFT_1_16, false),
          map(0x1F, MINECRAFT_1_16_2, false));
      clientbound.register(JoinGamePacket.class, JoinGamePacket::new,
          map(0x01, MINECRAFT_1_7_2, false),
          map(0x23, MINECRAFT_1_9, false),
          map(0x25, MINECRAFT_1_13, false),
          map(0x25, MINECRAFT_1_14, false),
          map(0x26, MINECRAFT_1_15, false),
          map(0x25, MINECRAFT_1_16, false),
          map(0x24, MINECRAFT_1_16_2, false));
      clientbound.register(RespawnPacket.class, RespawnPacket::new,
          map(0x07, MINECRAFT_1_7_2, true),
          map(0x33, MINECRAFT_1_9, true),
          map(0x34, MINECRAFT_1_12, true),
          map(0x35, MINECRAFT_1_12_1, true),
          map(0x38, MINECRAFT_1_13, true),
          map(0x3A, MINECRAFT_1_14, true),
          map(0x3B, MINECRAFT_1_15, true),
          map(0x3A, MINECRAFT_1_16, true),
          map(0x39, MINECRAFT_1_16_2, true));
      clientbound.register(ResourcePackRequestPacket.class, ResourcePackRequestPacket::new,
          map(0x48, MINECRAFT_1_8, true),
          map(0x32, MINECRAFT_1_9, true),
          map(0x33, MINECRAFT_1_12, true),
          map(0x34, MINECRAFT_1_12_1, true),
          map(0x37, MINECRAFT_1_13, true),
          map(0x39, MINECRAFT_1_14, true),
          map(0x3A, MINECRAFT_1_15, true),
          map(0x39, MINECRAFT_1_16, true),
          map(0x38, MINECRAFT_1_16_2, true));
      clientbound.register(HeaderAndFooterPacket.class, HeaderAndFooterPacket::new,
          map(0x47, MINECRAFT_1_8, true),
          map(0x48, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_9_4, true),
          map(0x49, MINECRAFT_1_12, true),
          map(0x4A, MINECRAFT_1_12_1, true),
          map(0x4E, MINECRAFT_1_13, true),
          map(0x53, MINECRAFT_1_14, true),
          map(0x54, MINECRAFT_1_15, true),
          map(0x53, MINECRAFT_1_16, true));
      clientbound.register(TitlePacket.class, TitlePacket::new,
          map(0x45, MINECRAFT_1_8, true),
          map(0x45, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_12, true),
          map(0x48, MINECRAFT_1_12_1, true),
          map(0x4B, MINECRAFT_1_13, true),
          map(0x4F, MINECRAFT_1_14, true),
          map(0x50, MINECRAFT_1_15, true),
          map(0x4F, MINECRAFT_1_16, true));
      clientbound.register(PlayerListItemPacket.class, PlayerListItemPacket::new,
          map(0x38, MINECRAFT_1_7_2, false),
          map(0x2D, MINECRAFT_1_9, false),
          map(0x2E, MINECRAFT_1_12_1, false),
          map(0x30, MINECRAFT_1_13, false),
          map(0x33, MINECRAFT_1_14, false),
          map(0x34, MINECRAFT_1_15, false),
          map(0x33, MINECRAFT_1_16, false),
          map(0x32, MINECRAFT_1_16_2, false));
    }
  },
  LOGIN {
    {
      serverbound.register(ServerLoginPacket.class, ServerLoginPacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
      serverbound.register(EncryptionResponsePacket.class, EncryptionResponsePacket::new,
          map(0x01, MINECRAFT_1_7_2, false));
      serverbound.register(LoginPluginResponsePacket.class, LoginPluginResponsePacket::new,
          map(0x02, MINECRAFT_1_13, false));
      clientbound.register(DisconnectPacket.class, DisconnectPacket::new,
          map(0x00, MINECRAFT_1_7_2, false));
      clientbound.register(EncryptionRequestPacket.class, EncryptionRequestPacket::new,
          map(0x01, MINECRAFT_1_7_2, false));
      clientbound.register(ServerLoginSuccessPacket.class, ServerLoginSuccessPacket::new,
          map(0x02, MINECRAFT_1_7_2, false));
      clientbound.register(SetCompressionPacket.class, SetCompressionPacket::new,
          map(0x03, MINECRAFT_1_8, false));
      clientbound.register(LoginPluginMessagePacket.class, LoginPluginMessagePacket::new,
          map(0x04, MINECRAFT_1_13, false));
    }
  };

  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;
  public final PacketRegistry clientbound = new PacketRegistry(ProtocolDirection.CLIENTBOUND);
  public final PacketRegistry serverbound = new PacketRegistry(ProtocolDirection.SERVERBOUND);

  public PacketRegistry.ProtocolRegistry getProtocolRegistry(ProtocolDirection direction,
      ProtocolVersion version) {
    return (direction == ProtocolDirection.SERVERBOUND ? this.serverbound : this.clientbound)
      .getProtocolRegistry(version);
  }

  public static class PacketRegistry {

    private final ProtocolDirection direction;
    private final Map<ProtocolVersion, ProtocolRegistry> versions;
    private boolean fallback = true;

    PacketRegistry(ProtocolDirection direction) {
      this.direction = direction;

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
        if (fallback) {
          return getProtocolRegistry(MINIMUM_VERSION);
        }
        throw new IllegalArgumentException("Could not find data for protocol version " + version);
      }
      return registry;
    }

    <P extends Packet> void register(Class<P> clazz, Supplier<P> packetSupplier,
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

          if (registry.packetIdToSupplier.containsKey(current.id)) {
            throw new IllegalArgumentException("Can not register class " + clazz.getSimpleName()
                + " with id " + current.id + " for " + registry.version
                + " because another packet is already registered");
          }

          if (registry.packetClassToId.containsKey(clazz)) {
            throw new IllegalArgumentException(clazz.getSimpleName()
                + " is already registered for version " + registry.version);
          }

          if (!current.encodeOnly) {
            registry.packetIdToSupplier.put(current.id, packetSupplier);
          }
          registry.packetClassToId.put(clazz, current.id);
        }
      }
    }

    public class ProtocolRegistry {

      public final ProtocolVersion version;
      final IntObjectMap<Supplier<? extends Packet>> packetIdToSupplier =
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
       * @return the packet instance, or {@code null} if the ID is not registered
       */
      public @Nullable Packet createPacket(final int id) {
        final Supplier<? extends Packet> supplier = this.packetIdToSupplier.get(id);
        if (supplier == null) {
          return null;
        }
        return supplier.get();
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

    private final int id;
    private final ProtocolVersion protocolVersion;
    private final boolean encodeOnly;

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
