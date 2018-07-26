package com.velocitypowered.proxy.protocol;

import com.velocitypowered.proxy.protocol.packets.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public enum StateRegistry {
    HANDSHAKE {
        {
            TO_SERVER.register(0x00, Handshake.class, Handshake::new);
        }
    },
    STATUS {
        {
            TO_SERVER.register(0x00, StatusRequest.class, StatusRequest::new);
            TO_SERVER.register(0x01, Ping.class, Ping::new);

            TO_CLIENT.register(0x00, StatusResponse.class, StatusResponse::new);
            TO_CLIENT.register(0x01, Ping.class, Ping::new);
        }
    },
    PLAY {
        {
            TO_SERVER.register(0x02, Chat.class, Chat::new);
            TO_SERVER.register(0x0b, Ping.class, Ping::new);

            TO_CLIENT.register(0x0F, Chat.class, Chat::new);
            TO_CLIENT.register(0x1A, Disconnect.class, Disconnect::new);
            TO_CLIENT.register(0x1F, Ping.class, Ping::new);
        }
    },
    LOGIN {
        {
            TO_SERVER.register(0x00, ServerLogin.class, ServerLogin::new);
            TO_SERVER.register(0x01, EncryptionResponse.class, EncryptionResponse::new);

            TO_CLIENT.register(0x00, Disconnect.class, Disconnect::new);
            TO_CLIENT.register(0x01, EncryptionRequest.class, EncryptionRequest::new);
            TO_CLIENT.register(0x02, ServerLoginSuccess.class, ServerLoginSuccess::new);
            TO_CLIENT.register(0x03, SetCompression.class, SetCompression::new);
        }
    };

    public final ProtocolMappings TO_CLIENT = new ProtocolMappings(ProtocolConstants.Direction.TO_CLIENT, this);
    public final ProtocolMappings TO_SERVER = new ProtocolMappings(ProtocolConstants.Direction.TO_SERVER, this);

    public static class ProtocolMappings {
        private final ProtocolConstants.Direction direction;
        private final StateRegistry state;
        private final Map<Integer, Supplier<? extends MinecraftPacket>> idsToSuppliers = new HashMap<>();
        private final Map<Class<? extends MinecraftPacket>, Integer> packetClassesToIds = new HashMap<>();

        public ProtocolMappings(ProtocolConstants.Direction direction, StateRegistry state) {
            this.direction = direction;
            this.state = state;
        }

        public <P extends MinecraftPacket> void register(int id, Class<P> clazz, Supplier<P> packetSupplier) {
            idsToSuppliers.put(id, packetSupplier);
            packetClassesToIds.put(clazz, id);
        }

        public MinecraftPacket createPacket(int id) {
            Supplier<? extends MinecraftPacket> supplier = idsToSuppliers.get(id);
            if (supplier == null) {
                return null;
            }
            return supplier.get();
        }

        public int getId(MinecraftPacket packet) {
            Integer id = packetClassesToIds.get(packet.getClass());
            if (id == null) {
                throw new IllegalArgumentException("Supplied packet " + packet.getClass().getName() + " doesn't have a mapping. Direction " + direction + " State " + state);
            }
            return id;
        }
    }
}
