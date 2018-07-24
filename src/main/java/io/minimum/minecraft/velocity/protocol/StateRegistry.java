package io.minimum.minecraft.velocity.protocol;

import io.minimum.minecraft.velocity.protocol.packets.*;

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

    },
    LOGIN {
        {
            TO_SERVER.register(0x00, ServerLogin.class, ServerLogin::new);

            TO_CLIENT.register(0x00, Disconnect.class, Disconnect::new);
            // Encryption Success will follow once Mojang auth/encryption is done
            TO_CLIENT.register(0x02, ServerLoginSuccess.class, ServerLogin::new);
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

        public void register(int id, Class<? extends MinecraftPacket> clazz, Supplier<? extends MinecraftPacket> packetSupplier) {
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
