package com.velocitypowered.proxy.protocol;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.proxy.protocol.packets.*;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import static com.velocitypowered.proxy.protocol.ProtocolConstants.MINECRAFT_1_12;

public enum StateRegistry {
    HANDSHAKE {
        {
            TO_SERVER.register(Handshake.class, Handshake::new,
                    generic(0x00));
        }
    },
    STATUS {
        {
            TO_SERVER.register(StatusRequest.class, StatusRequest::new,
                    generic(0x00));
            TO_SERVER.register(Ping.class, Ping::new,
                    generic(0x01));

            TO_CLIENT.register(StatusResponse.class, StatusResponse::new,
                    generic(0x00));
            TO_CLIENT.register(Ping.class, Ping::new,
                    generic(0x01));
        }
    },
    PLAY {
        {
            TO_SERVER.register(Chat.class, Chat::new,
                    map(0x02, MINECRAFT_1_12));
            TO_SERVER.register(Ping.class, Ping::new,
                    map(0x0b, MINECRAFT_1_12));

            TO_CLIENT.register(Chat.class, Chat::new,
                    map(0x0F, MINECRAFT_1_12));
            TO_CLIENT.register(Disconnect.class, Disconnect::new,
                    map(0x1A, MINECRAFT_1_12));
            TO_CLIENT.register(Ping.class, Ping::new,
                    map(0x1F, MINECRAFT_1_12));
            TO_CLIENT.register(JoinGame.class, JoinGame::new,
                    map(0x23, MINECRAFT_1_12));
            TO_CLIENT.register(Respawn.class, Respawn::new,
                    map(0x35, MINECRAFT_1_12));
        }
    },
    LOGIN {
        {
            TO_SERVER.register(ServerLogin.class, ServerLogin::new,
                    generic(0x00));
            TO_SERVER.register(EncryptionResponse.class, EncryptionResponse::new,
                    generic(0x01));

            TO_CLIENT.register(Disconnect.class, Disconnect::new,
                    generic(0x00));
            TO_CLIENT.register(EncryptionRequest.class, EncryptionRequest::new,
                    generic(0x01));
            TO_CLIENT.register(ServerLoginSuccess.class, ServerLoginSuccess::new,
                    generic(0x02));
            TO_CLIENT.register(SetCompression.class, SetCompression::new,
                    generic(0x03));
        }
    };

    public final PacketRegistry TO_CLIENT = new PacketRegistry(ProtocolConstants.Direction.TO_CLIENT, this);
    public final PacketRegistry TO_SERVER = new PacketRegistry(ProtocolConstants.Direction.TO_SERVER, this);

    public static class PacketRegistry {
        private final ProtocolConstants.Direction direction;
        private final StateRegistry state;
        private final IntObjectMap<IntObjectMap<Supplier<? extends MinecraftPacket>>> byProtocolVersionToProtocolIds = new IntObjectHashMap<>();
        private final Map<Class<? extends MinecraftPacket>, List<PacketMapping>> idMappers = new HashMap<>();

        public PacketRegistry(ProtocolConstants.Direction direction, StateRegistry state) {
            this.direction = direction;
            this.state = state;
        }

        public <P extends MinecraftPacket> void register(Class<P> clazz, Supplier<P> packetSupplier, PacketMapping... mappings) {
            if (mappings.length == 0) {
                throw new IllegalArgumentException("At least one mapping must be provided.");
            }
            for (PacketMapping mapping : mappings) {
                IntObjectMap<Supplier<? extends MinecraftPacket>> ids = byProtocolVersionToProtocolIds.get(mapping.protocolVersion);
                if (ids == null) {
                    byProtocolVersionToProtocolIds.put(mapping.protocolVersion, ids = new IntObjectHashMap<>());
                }
                ids.put(mapping.id, packetSupplier);
            }
            idMappers.put(clazz, ImmutableList.copyOf(mappings));
        }

        public MinecraftPacket createPacket(int id, int protocolVersion) {
            IntObjectMap<Supplier<? extends MinecraftPacket>> bestLookup = null;
            for (IntObjectMap.PrimitiveEntry<IntObjectMap<Supplier<? extends MinecraftPacket>>> entry : byProtocolVersionToProtocolIds.entries()) {
                if (entry.key() <= protocolVersion) {
                    bestLookup = entry.value();
                }
            }
            if (bestLookup == null) {
                return null;
            }
            Supplier<? extends MinecraftPacket> supplier = bestLookup.get(id);
            if (supplier == null) {
                return null;
            }
            return supplier.get();
        }

        public int getId(MinecraftPacket packet, int protocolVersion) {
            Preconditions.checkNotNull(packet, "packet");

            List<PacketMapping> mappings = idMappers.get(packet.getClass());
            if (mappings == null || mappings.isEmpty()) {
                throw new IllegalArgumentException("Supplied packet " + packet.getClass().getName() +
                        " doesn't have any mappings. Direction " + direction + " State " + state);
            }
            int useId = -1;
            for (PacketMapping mapping : mappings) {
                if (mapping.protocolVersion <= protocolVersion) {
                    useId = mapping.id;
                }
            }
            if (useId == -1) {
                throw new IllegalArgumentException("Unable to find a mapping for " + packet.getClass().getName()
                        + " Version " + protocolVersion + " Direction " + direction + " State " + state);
            }
            return useId;
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
