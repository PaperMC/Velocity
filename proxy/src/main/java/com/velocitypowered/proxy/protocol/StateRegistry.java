package com.velocitypowered.proxy.protocol;

import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_10;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_11;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_11_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_12_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_13_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_14;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_8;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_1;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_2;
import static com.velocitypowered.api.network.ProtocolVersion.MINECRAFT_1_9_4;
import static com.velocitypowered.api.network.ProtocolVersion.MINIMUM_VERSION;
import static com.velocitypowered.proxy.protocol.ProtocolUtils.Direction;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.proxy.protocol.packet.AvailableCommands;
import com.velocitypowered.proxy.protocol.packet.BossBar;
import com.velocitypowered.proxy.protocol.packet.Chat;
import com.velocitypowered.proxy.protocol.packet.ClientSettings;
import com.velocitypowered.proxy.protocol.packet.Disconnect;
import com.velocitypowered.proxy.protocol.packet.EncryptionRequest;
import com.velocitypowered.proxy.protocol.packet.EncryptionResponse;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.HeaderAndFooter;
import com.velocitypowered.proxy.protocol.packet.JoinGame;
import com.velocitypowered.proxy.protocol.packet.KeepAlive;
import com.velocitypowered.proxy.protocol.packet.LoginPluginMessage;
import com.velocitypowered.proxy.protocol.packet.LoginPluginResponse;
import com.velocitypowered.proxy.protocol.packet.PlayerListItem;
import com.velocitypowered.proxy.protocol.packet.PluginMessage;
import com.velocitypowered.proxy.protocol.packet.ResourcePackRequest;
import com.velocitypowered.proxy.protocol.packet.ResourcePackResponse;
import com.velocitypowered.proxy.protocol.packet.Respawn;
import com.velocitypowered.proxy.protocol.packet.ServerLogin;
import com.velocitypowered.proxy.protocol.packet.ServerLoginSuccess;
import com.velocitypowered.proxy.protocol.packet.SetCompression;
import com.velocitypowered.proxy.protocol.packet.StatusPing;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;
import com.velocitypowered.proxy.protocol.packet.TabCompleteRequest;
import com.velocitypowered.proxy.protocol.packet.TabCompleteResponse;
import com.velocitypowered.proxy.protocol.packet.TitlePacket;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Collection;
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
      serverbound.register(Handshake.class, Handshake::new,
          genericMappings(0x00));
    }
  },
  STATUS {
    {
      serverbound.register(StatusRequest.class, () -> StatusRequest.INSTANCE,
          genericMappings(0x00));
      serverbound.register(StatusPing.class, StatusPing::new,
          genericMappings(0x01));

      clientbound.register(StatusResponse.class, StatusResponse::new,
          genericMappings(0x00));
      clientbound.register(StatusPing.class, StatusPing::new,
          genericMappings(0x01));
    }
  },
  PLAY {
    {
      serverbound.fallback = false;
      clientbound.fallback = false;

      serverbound.register(TabCompleteRequest.class, TabCompleteRequest::new,
          map(0x14, MINECRAFT_1_8, false),
          map(0x01, MINECRAFT_1_9, false),
          map(0x02, MINECRAFT_1_12, false),
          map(0x01, MINECRAFT_1_12_1, false),
          map(0x05, MINECRAFT_1_13, false),
          map(0x06, MINECRAFT_1_14, false));  
      serverbound.register(Chat.class, Chat::new,
          map(0x01, MINECRAFT_1_8, false),
          map(0x02, MINECRAFT_1_9, false),
          map(0x03, MINECRAFT_1_12, false),
          map(0x02, MINECRAFT_1_12_1, false),
          map(0x02, MINECRAFT_1_13, false),
          map(0x03, MINECRAFT_1_14, false));
      serverbound.register(ClientSettings.class, ClientSettings::new,
          map(0x15, MINECRAFT_1_8, false),
          map(0x04, MINECRAFT_1_9, false),
          map(0x05, MINECRAFT_1_12, false),
          map(0x04, MINECRAFT_1_12_1, false),
          map(0x04, MINECRAFT_1_13, false),
          map(0x05, MINECRAFT_1_14, false));
      serverbound.register(PluginMessage.class, PluginMessage::new,
          map(0x17, MINECRAFT_1_8, false),
          map(0x09, MINECRAFT_1_9, false),
          map(0x0A, MINECRAFT_1_12, false),
          map(0x09, MINECRAFT_1_12_1, false),
          map(0x0A, MINECRAFT_1_13, false),
          map(0x0B, MINECRAFT_1_14, false));
      serverbound.register(KeepAlive.class, KeepAlive::new,
          map(0x00, MINECRAFT_1_8, false),
          map(0x0B, MINECRAFT_1_9, false),
          map(0x0C, MINECRAFT_1_12, false),
          map(0x0B, MINECRAFT_1_12_1, false),
          map(0x0E, MINECRAFT_1_13, false),
          map(0x0F, MINECRAFT_1_14, false));
      serverbound.register(ResourcePackResponse.class, ResourcePackResponse::new,
          map(0x19, MINECRAFT_1_8, false),
          map(0x16, MINECRAFT_1_9, false),
          map(0x18, MINECRAFT_1_12, false),
          map(0x1D, MINECRAFT_1_13, false),
          map(0x1F, MINECRAFT_1_14, false));

      clientbound.register(BossBar.class, BossBar::new,
          map(0x0C, MINECRAFT_1_9, false),
          map(0x0C, MINECRAFT_1_12, false),
          map(0x0C, MINECRAFT_1_13, false),
          map(0x0C, MINECRAFT_1_14, false));
      clientbound.register(Chat.class, Chat::new,
          map(0x02, MINECRAFT_1_8, true),
          map(0x0F, MINECRAFT_1_9, true),
          map(0x0F, MINECRAFT_1_12, true),
          map(0x0E, MINECRAFT_1_13, true),
          map(0x0E, MINECRAFT_1_14, false));
      clientbound.register(TabCompleteResponse.class, TabCompleteResponse::new,
          map(0x3A, MINECRAFT_1_8, false),
          map(0x0E, MINECRAFT_1_9, false),
          map(0x0E, MINECRAFT_1_12, false),
          map(0x10, MINECRAFT_1_13, false),
          map(0x10, MINECRAFT_1_14, false));
      clientbound.register(AvailableCommands.class, AvailableCommands::new,
          map(0x11, MINECRAFT_1_13, false),
          map(0x11, MINECRAFT_1_14, false));
      clientbound.register(PluginMessage.class, PluginMessage::new,
          map(0x3F, MINECRAFT_1_8, false),
          map(0x18, MINECRAFT_1_9, false),
          map(0x18, MINECRAFT_1_12, false),
          map(0x19, MINECRAFT_1_13, false),
          map(0x18, MINECRAFT_1_14, false));
      clientbound.register(Disconnect.class, Disconnect::new,
          map(0x40, MINECRAFT_1_8, false),
          map(0x1A, MINECRAFT_1_9, false),
          map(0x1A, MINECRAFT_1_12, false),
          map(0x1B, MINECRAFT_1_13, false),
          map(0x1A, MINECRAFT_1_14, false));
      clientbound.register(KeepAlive.class, KeepAlive::new,
          map(0x00, MINECRAFT_1_8, false),
          map(0x1F, MINECRAFT_1_9, false),
          map(0x1F, MINECRAFT_1_12, false),
          map(0x21, MINECRAFT_1_13, false),
          map(0x20, MINECRAFT_1_14, false));
      clientbound.register(JoinGame.class, JoinGame::new,
          map(0x01, MINECRAFT_1_8, false),
          map(0x23, MINECRAFT_1_9, false),
          map(0x23, MINECRAFT_1_12, false),
          map(0x25, MINECRAFT_1_13, false),
          map(0x25, MINECRAFT_1_14, false));
      clientbound.register(Respawn.class, Respawn::new,
          map(0x07, MINECRAFT_1_8, true),
          map(0x33, MINECRAFT_1_9, true),
          map(0x34, MINECRAFT_1_12, true),
          map(0x35, MINECRAFT_1_12_1, true),
          map(0x38, MINECRAFT_1_13, true),
          map(0x3A, MINECRAFT_1_14, true));
      clientbound.register(ResourcePackRequest.class, ResourcePackRequest::new,
          map(0x48, MINECRAFT_1_8, true),
          map(0x32, MINECRAFT_1_9, true),
          map(0x33, MINECRAFT_1_12, true),
          map(0x34, MINECRAFT_1_12_1, true),
          map(0x37, MINECRAFT_1_13, true),
          map(0x39, MINECRAFT_1_14, true));
      clientbound.register(HeaderAndFooter.class, HeaderAndFooter::new,
          map(0x47, MINECRAFT_1_8, true),
          map(0x48, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_9_4, true),
          map(0x49, MINECRAFT_1_12, true),
          map(0x4A, MINECRAFT_1_12_1, true),
          map(0x4E, MINECRAFT_1_13, true),
          map(0x53, MINECRAFT_1_14, true));
      clientbound.register(TitlePacket.class, TitlePacket::new,
          map(0x45, MINECRAFT_1_8, true),
          map(0x45, MINECRAFT_1_9, true),
          map(0x47, MINECRAFT_1_12, true),
          map(0x48, MINECRAFT_1_12_1, true),
          map(0x4B, MINECRAFT_1_13, true),
          map(0x4F, MINECRAFT_1_14, true));
      clientbound.register(PlayerListItem.class, PlayerListItem::new,
          map(0x38, MINECRAFT_1_8, false),
          map(0x2D, MINECRAFT_1_9, false),
          map(0x2D, MINECRAFT_1_12, false),
          map(0x2E, MINECRAFT_1_12_1, false),
          map(0x30, MINECRAFT_1_13, false),
          map(0x33, MINECRAFT_1_14, false));
    }
  },
  LOGIN {
    {
      serverbound.register(ServerLogin.class, ServerLogin::new,
          genericMappings(0x00));
      serverbound.register(EncryptionResponse.class, EncryptionResponse::new,
          genericMappings(0x01));
      serverbound.register(LoginPluginResponse.class, LoginPluginResponse::new,
          map(0x02, MINECRAFT_1_13, false),
          map(0x02, MINECRAFT_1_14, false));

      clientbound.register(Disconnect.class, Disconnect::new,
          genericMappings(0x00));
      clientbound.register(EncryptionRequest.class, EncryptionRequest::new,
          genericMappings(0x01));
      clientbound.register(ServerLoginSuccess.class, ServerLoginSuccess::new,
          genericMappings(0x02));
      clientbound.register(SetCompression.class, SetCompression::new,
          genericMappings(0x03));
      clientbound.register(LoginPluginMessage.class, LoginPluginMessage::new,
          map(0x04, MINECRAFT_1_13, false),
          map(0x04, MINECRAFT_1_14, false));
    }
  };

  public static final int STATUS_ID = 1;
  public static final int LOGIN_ID = 2;
  public final PacketRegistry clientbound = new PacketRegistry(Direction.CLIENTBOUND);
  public final PacketRegistry serverbound = new PacketRegistry(Direction.SERVERBOUND);

  public static class PacketRegistry {

    private static final Map<ProtocolVersion, Collection<ProtocolVersion>> LINKED_PROTOCOL_VERSIONS
        = new EnumMap<>(ProtocolVersion.class);

    static {
      LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_9, EnumSet.of(MINECRAFT_1_9_1, MINECRAFT_1_9_2,
          MINECRAFT_1_9_4));
      LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_9_4, EnumSet.of(MINECRAFT_1_10, MINECRAFT_1_11,
          MINECRAFT_1_11_1));
      LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_12, EnumSet.of(MINECRAFT_1_12_1));
      LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_12_1, EnumSet.of(MINECRAFT_1_12_2));
      LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_13, EnumSet.of(MINECRAFT_1_13_1, MINECRAFT_1_13_2));
      //for 1.14 ???
    }

    private final Direction direction;
    private final Map<ProtocolVersion, ProtocolRegistry> versions;
    private boolean fallback = true;

    PacketRegistry(Direction direction) {
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

    <P extends MinecraftPacket> void register(Class<P> clazz, Supplier<P> packetSupplier,
        PacketMapping... mappings) {
      if (mappings.length == 0) {
        throw new IllegalArgumentException("At least one mapping must be provided.");
      }

      for (final PacketMapping mapping : mappings) {
        ProtocolRegistry registry = this.versions.get(mapping.protocolVersion);
        if (registry == null) {
          throw new IllegalArgumentException("Unknown protocol version " + mapping.protocolVersion);
        }
        if (!mapping.encodeOnly) {
          registry.packetIdToSupplier.put(mapping.id, packetSupplier);
        }
        registry.packetClassToId.put(clazz, mapping.id);

        Collection<ProtocolVersion> linked = LINKED_PROTOCOL_VERSIONS.get(mapping.protocolVersion);
        if (linked != null) {
          links:
          for (ProtocolVersion linkedVersion : linked) {
            // Make sure that later mappings override this one.
            for (PacketMapping m : mappings) {
              if (linkedVersion == m.protocolVersion) {
                continue links;
              }
            }
            register(clazz, packetSupplier, map(mapping.id, linkedVersion, mapping.encodeOnly));
          }
        }
      }
    }

    public class ProtocolRegistry {

      public final ProtocolVersion version;
      final IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier =
          new IntObjectHashMap<>(16, 0.5f);
      final Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId =
          new Object2IntOpenHashMap<>(16, 0.5f);

      ProtocolRegistry(final ProtocolVersion version) {
        this.version = version;
        this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
      }

      /**
       * Attempts to create a packet from the specified {@code id}.
       * @param id the packet ID
       * @return the packet instance, or {@code null} if the ID is not registered
       */
      public @Nullable MinecraftPacket createPacket(final int id) {
        final Supplier<? extends MinecraftPacket> supplier = this.packetIdToSupplier.get(id);
        if (supplier == null) {
          return null;
        }
        return supplier.get();
      }

      /**
       * Attempts to look up the packet ID for an {@code packet}.
       * @param packet the packet to look up
       * @return the packet ID
       * @throws IllegalArgumentException if the packet ID is not found
       */
      public int getPacketId(final MinecraftPacket packet) {
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
   * @param id Packet Id
   * @param version Protocol version
   * @param encodeOnly When true packet decoding will be disabled
   * @return PacketMapping with the provided arguments
   */
  private static PacketMapping map(int id, ProtocolVersion version, boolean encodeOnly) {
    return new PacketMapping(id, version, encodeOnly);
  }

  private static PacketMapping[] genericMappings(int id) {
    return new PacketMapping[]{
        map(id, MINECRAFT_1_8, false),
        map(id, MINECRAFT_1_9, false),
        map(id, MINECRAFT_1_12, false),
        map(id, MINECRAFT_1_13, false),
        map(id, MINECRAFT_1_14, false)
    };
  }
}
