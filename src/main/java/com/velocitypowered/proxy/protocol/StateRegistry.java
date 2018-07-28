package com.velocitypowered.proxy.protocol;

import com.velocitypowered.proxy.protocol.packets.*;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.velocitypowered.proxy.protocol.ProtocolConstants.MINECRAFT_1_12;

public enum StateRegistry {
    HANDSHAKE {
        {
            SERVERBOUND.register(Handshake.class, Handshake::new,
                    generic(0x00));
        }
    },
    STATUS {
        {
            SERVERBOUND.register(StatusRequest.class, StatusRequest::new,
                    generic(0x00));
            SERVERBOUND.register(Ping.class, Ping::new,
                    generic(0x01));

            CLIENTBOUND.register(StatusResponse.class, StatusResponse::new,
                    generic(0x00));
            CLIENTBOUND.register(Ping.class, Ping::new,
                    generic(0x01));
        }
    },
    PLAY {
        {
            SERVERBOUND.register(Chat.class, Chat::new,
                    map(0x02, MINECRAFT_1_12));
            SERVERBOUND.register(Ping.class, Ping::new,
                    map(0x0b, MINECRAFT_1_12));

            CLIENTBOUND.register(Chat.class, Chat::new,
                    map(0x0F, MINECRAFT_1_12));
            CLIENTBOUND.register(Disconnect.class, Disconnect::new,
                    map(0x1A, MINECRAFT_1_12));
            CLIENTBOUND.register(Ping.class, Ping::new,
                    map(0x1F, MINECRAFT_1_12));
            CLIENTBOUND.register(JoinGame.class, JoinGame::new,
                    map(0x23, MINECRAFT_1_12));
            CLIENTBOUND.register(Respawn.class, Respawn::new,
                    map(0x35, MINECRAFT_1_12));
        }
    },
    LOGIN {
        {
            SERVERBOUND.register(ServerLogin.class, ServerLogin::new,
                    generic(0x00));
            SERVERBOUND.register(EncryptionResponse.class, EncryptionResponse::new,
                    generic(0x01));

            CLIENTBOUND.register(Disconnect.class, Disconnect::new,
                    generic(0x00));
            CLIENTBOUND.register(EncryptionRequest.class, EncryptionRequest::new,
                    generic(0x01));
            CLIENTBOUND.register(ServerLoginSuccess.class, ServerLoginSuccess::new,
                    generic(0x02));
            CLIENTBOUND.register(SetCompression.class, SetCompression::new,
                    generic(0x03));
        }
    };

    public final PacketRegistry CLIENTBOUND = new PacketRegistry(ProtocolConstants.Direction.CLIENTBOUND);
    public final PacketRegistry SERVERBOUND = new PacketRegistry(ProtocolConstants.Direction.SERVERBOUND);

    public static class PacketRegistry {
        private final ProtocolConstants.Direction direction;
        private final IntObjectMap<ProtocolVersion> versions = new IntObjectHashMap<>();

        public PacketRegistry(ProtocolConstants.Direction direction) {
            this.direction = direction;
        }

        public ProtocolVersion getVersion(final int version) {
            ProtocolVersion result = null;
            for (final IntObjectMap.PrimitiveEntry<ProtocolVersion> entry : this.versions.entries()) {
                if (entry.key() <= version) {
                    result = entry.value();
                }
            }
            if (result == null) {
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
                    version = new ProtocolVersion(mapping.protocolVersion);
                    this.versions.put(mapping.protocolVersion, version);
                }
                version.packetIdToSupplier.put(mapping.id, packetSupplier);
                version.packetClassToId.put(clazz, mapping.id);
            }
        }

        public class ProtocolVersion {
            public final int id;
            final IntObjectMap<Supplier<? extends MinecraftPacket>> packetIdToSupplier = new IntObjectHashMap<>();
            final Map<Class<? extends MinecraftPacket>, Integer> packetClassToId = new HashMap<>();

            ProtocolVersion(final int id) {
                this.id = id;
            }

            public MinecraftPacket createPacket(final int id) {
                final Supplier<? extends MinecraftPacket> supplier = this.packetIdToSupplier.get(id);
                if (supplier == null) {
                    return null;
                }
                return supplier.get();
            }

            public int getPacketId(final MinecraftPacket packet) {
                final Integer id = this.packetClassToId.get(packet.getClass());
                if (id == null) {
                    throw new IllegalArgumentException(String.format(
                            "Unable to find id for packet of type %s in %s protocol %s",
                            packet.getClass().getName(), PacketRegistry.this.direction, this.id
                    ));
                }
                return id;
            }
        }
    }

    public static class PacketMapping {
        private final int id;
        private final int protocolVersion;

        public PacketMapping(int id, int protocolVersion) {
            this.id = id;
            this.protocolVersion = protocolVersion;
        }

        @Override
        public String toString() {
            return "PacketMapping{" +
                    "id=" + id +
                    ", protocolVersion=" + protocolVersion +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PacketMapping that = (PacketMapping) o;
            return id == that.id &&
                    protocolVersion == that.protocolVersion;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, protocolVersion);
        }
    }

    private static PacketMapping map(int id, int version) {
        return new PacketMapping(id, version);
    }

    private static PacketMapping generic(int id) {
        return new PacketMapping(id, 0);
    }
}
