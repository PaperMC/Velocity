package com.velocitypowered.proxy.protocol;

import com.google.common.base.Strings;
import com.google.common.primitives.ImmutableIntArray;
import com.velocitypowered.proxy.protocol.packet.*;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.util.Objects;
import java.util.function.Supplier;

import static com.velocitypowered.proxy.protocol.ProtocolConstants.*;

public enum StateRegistry {
        
    HANDSHAKE {
        {
            SERVERBOUND.register(Handshake.class, Handshake::new,
                    genericMappings(0x00));
        }
    },
    STATUS {
        {
            SERVERBOUND.register(StatusRequest.class, () -> StatusRequest.INSTANCE,
                    genericMappings(0x00));
            SERVERBOUND.register(StatusPing.class, StatusPing::new,
                    genericMappings(0x01));

            CLIENTBOUND.register(StatusResponse.class, StatusResponse::new,
                    genericMappings(0x00));
            CLIENTBOUND.register(StatusPing.class, StatusPing::new,
                    genericMappings(0x01));
        }
    },
    PLAY {
        {
            SERVERBOUND.register(TabCompleteRequest.class, TabCompleteRequest::new,
                    map(0x14, MINECRAFT_1_8, false),
                    map(0x01, MINECRAFT_1_9, false),
                    map(0x02, MINECRAFT_1_12, false),
                    map(0x01, MINECRAFT_1_12_1, false));
            SERVERBOUND.register(Chat.class, Chat::new,
                    map(0x01, MINECRAFT_1_8, false),
                    map(0x02, MINECRAFT_1_9, false),
                    map(0x03, MINECRAFT_1_12, false),
                    map(0x02, MINECRAFT_1_12_1, false),
                    map(0x02, MINECRAFT_1_13, false));
            SERVERBOUND.register(ClientSettings.class, ClientSettings::new,
                    map(0x15, MINECRAFT_1_8, false),
                    map(0x04, MINECRAFT_1_9, false),
                    map(0x05, MINECRAFT_1_12, false),
                    map(0x04, MINECRAFT_1_12_1, false),
                    map(0x04, MINECRAFT_1_13, false));
            SERVERBOUND.register(PluginMessage.class, PluginMessage::new,
                    map(0x17, MINECRAFT_1_8, false),
                    map(0x09, MINECRAFT_1_9, false),
                    map(0x0A, MINECRAFT_1_12, false),
                    map(0x09, MINECRAFT_1_12_1, false),
                    map(0x0A, MINECRAFT_1_13, false));
            SERVERBOUND.register(KeepAlive.class, KeepAlive::new,
                    map(0x00, MINECRAFT_1_8, false),
                    map(0x0B, MINECRAFT_1_9, false),
                    map(0x0C, MINECRAFT_1_12, false),
                    map(0x0B, MINECRAFT_1_12_1, false),
                    map(0x0E, MINECRAFT_1_13, false));

            CLIENTBOUND.register(BossBar.class, BossBar::new,
                    map(0x0C, MINECRAFT_1_9, false),
                    map(0x0C, MINECRAFT_1_12, false),
                    map(0x0C, MINECRAFT_1_13, false));
            CLIENTBOUND.register(Chat.class, Chat::new,
                    map(0x02, MINECRAFT_1_8, true),
                    map(0x0F, MINECRAFT_1_9, true),
                    map(0x0F, MINECRAFT_1_12, true),
                    map(0x0E, MINECRAFT_1_13, true));
            CLIENTBOUND.register(TabCompleteResponse.class, TabCompleteResponse::new,
                    map(0x3A, MINECRAFT_1_8, false),
                    map(0x0E, MINECRAFT_1_9, false),
                    map(0x0E, MINECRAFT_1_12, false));
            CLIENTBOUND.register(PluginMessage.class, PluginMessage::new,
                    map(0x3F, MINECRAFT_1_8, false),
                    map(0x18, MINECRAFT_1_9, false),
                    map(0x18, MINECRAFT_1_12, false),
                    map(0x19, MINECRAFT_1_13, false));
            CLIENTBOUND.register(Disconnect.class, Disconnect::new,
                    map(0x40, MINECRAFT_1_8, false),
                    map(0x1A, MINECRAFT_1_9, false),
                    map(0x1A, MINECRAFT_1_12, false),
                    map(0x1B, MINECRAFT_1_13, false));
            CLIENTBOUND.register(KeepAlive.class, KeepAlive::new,
                    map(0x00, MINECRAFT_1_8, false),
                    map(0x1F, MINECRAFT_1_9, false),
                    map(0x1F, MINECRAFT_1_12, false),
                    map(0x21, MINECRAFT_1_13, false));
            CLIENTBOUND.register(JoinGame.class, JoinGame::new,
                    map(0x01, MINECRAFT_1_8, false),
                    map(0x23, MINECRAFT_1_9, false),
                    map(0x23, MINECRAFT_1_12, false),
                    map(0x25, MINECRAFT_1_13, false));
            CLIENTBOUND.register(Respawn.class, Respawn::new,
                    map(0x07, MINECRAFT_1_8, true),
                    map(0x33, MINECRAFT_1_9, true),
                    map(0x34, MINECRAFT_1_12, true),
                    map(0x35, MINECRAFT_1_12_2, true),
                    map(0x38, MINECRAFT_1_13, true));
            CLIENTBOUND.register(HeaderAndFooter.class, HeaderAndFooter::new,
                    map(0x47, MINECRAFT_1_8, true),
                    map(0x48, MINECRAFT_1_9, true),
                    map(0x47, MINECRAFT_1_9_4, true),
                    map(0x49, MINECRAFT_1_12, true),
                    map(0x4A, MINECRAFT_1_12_1, true),
                    map(0x4E, MINECRAFT_1_13, true));
            CLIENTBOUND.register(TitlePacket.class, TitlePacket::new,
                    map(0x45, MINECRAFT_1_8, true),
                    map(0x45, MINECRAFT_1_9, true),
                    map(0x47, MINECRAFT_1_12, true),
                    map(0x48, MINECRAFT_1_12_1, true),
                    map(0x4B, MINECRAFT_1_13, true));
            CLIENTBOUND.register(PlayerListItem.class, PlayerListItem::new,
                    map(0x38, MINECRAFT_1_8, false),
                    map(0x2D, MINECRAFT_1_9, false),
                    map(0x2D, MINECRAFT_1_12, false),
                    map(0x2E, MINECRAFT_1_12_1, false),
                    map(0x30, MINECRAFT_1_13, false));
        }
    },
    LOGIN {
        {
            SERVERBOUND.register(ServerLogin.class, ServerLogin::new,
                    genericMappings(0x00));
            SERVERBOUND.register(EncryptionResponse.class, EncryptionResponse::new,
                    genericMappings(0x01));
            SERVERBOUND.register(LoginPluginResponse.class, LoginPluginResponse::new,
                    map(0x02, MINECRAFT_1_13, false));

            CLIENTBOUND.register(Disconnect.class, Disconnect::new,
                    genericMappings(0x00));
            CLIENTBOUND.register(EncryptionRequest.class, EncryptionRequest::new,
                    genericMappings(0x01));
            CLIENTBOUND.register(ServerLoginSuccess.class, ServerLoginSuccess::new,
                    genericMappings(0x02));
            CLIENTBOUND.register(SetCompression.class, SetCompression::new,
                    genericMappings(0x03));
            CLIENTBOUND.register(LoginPluginMessage.class, LoginPluginMessage::new,
                    map(0x04, MINECRAFT_1_13, false));
        }
    };

    public static final int STATUS_ID = 1;
    public static final int LOGIN_ID = 2;
    public final PacketRegistry CLIENTBOUND = new PacketRegistry(ProtocolConstants.Direction.CLIENTBOUND, this);
    public final PacketRegistry SERVERBOUND = new PacketRegistry(ProtocolConstants.Direction.SERVERBOUND, this);

    public static class PacketRegistry {
        private static final IntObjectMap<ImmutableIntArray> LINKED_PROTOCOL_VERSIONS = new IntObjectHashMap<>();

        static {
            LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_9, ImmutableIntArray.of(MINECRAFT_1_9_1, MINECRAFT_1_9_2, MINECRAFT_1_9_4));
            LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_9_4, ImmutableIntArray.of(MINECRAFT_1_10, MINECRAFT_1_11, MINECRAFT_1_11_1));
            LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_12, ImmutableIntArray.of(MINECRAFT_1_12_1));
            LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_12_1, ImmutableIntArray.of(MINECRAFT_1_12_2));
            LINKED_PROTOCOL_VERSIONS.put(MINECRAFT_1_13, ImmutableIntArray.of(MINECRAFT_1_13_1, MINECRAFT_1_13_2));
        }

        private final ProtocolConstants.Direction direction;
        private final StateRegistry state;
        private final IntObjectMap<ProtocolVersion> versions = new IntObjectHashMap<>(16);

        public PacketRegistry(Direction direction, StateRegistry state) {
            this.direction = direction;
            this.state = state;
            ProtocolConstants.SUPPORTED_VERSIONS.forEach(version -> versions.put(version, new ProtocolVersion(version)));
        }

        public ProtocolVersion getVersion(final int version) {
            ProtocolVersion result = versions.get(version);
            if (result == null) {
                if (state != PLAY) {
                    return getVersion(MINIMUM_GENERIC_VERSION);
                }
                throw new IllegalArgumentException("Could not find data for protocol version " + version);
            }
            return result;
        }

        public <P extends MinecraftPacket> void register(Class<P> clazz, Supplier<P> packetSupplier, PacketMapping... mappings) {
            if (mappings.length == 0) {
                throw new IllegalArgumentException("At least one mapping must be provided.");
            }

            for (final PacketMapping mapping : mappings) {
                ProtocolVersion version = this.versions.get(mapping.protocolVersion);
                if (version == null) {
                    throw new IllegalArgumentException("Unknown protocol version " + mapping.protocolVersion);
                }
                if (!mapping.encodeOnly) {
                    version.packetIdToSupplier.put(mapping.id, packetSupplier);
                }
                version.packetClassToId.put(clazz, mapping.id);

                ImmutableIntArray linked = LINKED_PROTOCOL_VERSIONS.get(mapping.protocolVersion);
                if (linked != null) {
                    links: for (int i = 0; i < linked.length(); i++) {
                        int linkedVersion = linked.get(i);
                        // Make sure that later mappings override this one.
                        for (PacketMapping m : mappings) {
                            if (linkedVersion == m.protocolVersion) continue links;
                        }
                        register(clazz, packetSupplier, map(mapping.id, linkedVersion, mapping.encodeOnly));
                    }
                }
            }
        }

        public class ProtocolVersion {
            public final int id;
            final IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier = new IntObjectHashMap<>(16, 0.5f);
            final Object2IntMap<Class<? extends MinecraftPacket>> packetClassToId = new Object2IntOpenHashMap<>(16, 0.5f);

            ProtocolVersion(final int id) {
                this.id = id;
                this.packetClassToId.defaultReturnValue(Integer.MIN_VALUE);
            }

            public MinecraftPacket createPacket(final int id) {
                final Supplier<? extends MinecraftPacket> supplier = this.packetIdToSupplier.get(id);
                if (supplier == null) {
                    return null;
                }
                return supplier.get();
            }

            public int getPacketId(final MinecraftPacket packet) {
                final int id = this.packetClassToId.getInt(packet.getClass());
                if (id == Integer.MIN_VALUE) {
                    throw new IllegalArgumentException(String.format(
                            "Unable to find id for packet of type %s in %s protocol %s",
                            packet.getClass().getName(), PacketRegistry.this.direction, this.id
                    ));
                }
                return id;
            }

            @Override
            public String toString() {
                StringBuilder mappingAsString = new StringBuilder("{");
                for (Object2IntMap.Entry<Class<? extends MinecraftPacket>> entry : packetClassToId.object2IntEntrySet()) {
                    mappingAsString.append(entry.getKey().getSimpleName()).append(" -> ")
                            .append("0x")
                            .append(Strings.padStart(Integer.toHexString(entry.getIntValue()), 2, '0'))
                            .append(", ");
                }
                mappingAsString.setLength(mappingAsString.length() - 2);
                mappingAsString.append("}");
                return "ProtocolVersion{" +
                        "id=" + id +
                        ", packetClassToId=" + mappingAsString.toString() +
                        '}';
            }
        }
    }

    public static class PacketMapping {
        private final int id;
        private final int protocolVersion;
        private final boolean encodeOnly;
        
        public PacketMapping(int id, int protocolVersion, boolean packetDecoding) {
            this.id = id;
            this.protocolVersion = protocolVersion;
            this.encodeOnly = packetDecoding;
        }

        @Override
        public String toString() {
            return "PacketMapping{" +
                    "id=" + id +
                    ", protocolVersion=" + protocolVersion +
                    ", encodeOnly=" + encodeOnly +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PacketMapping that = (PacketMapping) o;
            return id == that.id &&
                    protocolVersion == that.protocolVersion &&
                    encodeOnly == that.encodeOnly;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, protocolVersion, encodeOnly);
        }
    }

    /**
     * Creates a PacketMapping using the provided arguments
     * @param id Packet Id
     * @param version Protocol version
     * @param encodeOnly When true packet decoding will be disabled
     * @return PacketMapping with the provided arguments
     */
    private static PacketMapping map(int id, int version, boolean encodeOnly) {
        return new PacketMapping(id, version, encodeOnly);
    }
    
    private static PacketMapping[] genericMappings(int id) {
        return new PacketMapping[]{
                map(id, MINECRAFT_1_8, false),
                map(id, MINECRAFT_1_9, false),
                map(id, MINECRAFT_1_12, false),
                map(id, MINECRAFT_1_13, false)
        };
    }
}
