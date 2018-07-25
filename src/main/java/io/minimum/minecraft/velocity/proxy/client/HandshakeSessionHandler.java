package io.minimum.minecraft.velocity.proxy.client;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.protocol.packets.Handshake;
import io.minimum.minecraft.velocity.proxy.MinecraftConnection;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;

public class HandshakeSessionHandler implements MinecraftSessionHandler {
    private final MinecraftConnection connection;

    public HandshakeSessionHandler(MinecraftConnection connection) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (!(packet instanceof Handshake)) {
            throw new IllegalArgumentException("Did not expect packet " + packet.getClass().getName());
        }

        Handshake handshake = (Handshake) packet;
        connection.setProtocolVersion(handshake.getProtocolVersion());
        switch (handshake.getNextStatus()) {
            case 1:
                // Status protocol
                connection.setState(StateRegistry.STATUS);
                connection.setSessionHandler(new StatusSessionHandler(connection));
                break;
            case 2:
                connection.setState(StateRegistry.LOGIN);
                connection.setSessionHandler(new LoginSessionHandler(connection));
                break;
            default:
                throw new IllegalArgumentException("Invalid state " + handshake.getNextStatus());
        }

    }
}
