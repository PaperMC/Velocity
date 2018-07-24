package io.minimum.minecraft.velocity.proxy.handler;

import com.google.common.base.Preconditions;
import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.Handshake;
import io.minimum.minecraft.velocity.proxy.InboundMinecraftConnection;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;

public class HandshakeSessionHandler implements MinecraftSessionHandler {
    private final InboundMinecraftConnection connection;

    public HandshakeSessionHandler(InboundMinecraftConnection connection) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (!(packet instanceof Handshake)) {
            throw new IllegalArgumentException("Did not expect packet " + packet.getClass().getName());
        }

        Handshake handshake = (Handshake) packet;
        connection.handleHandshake(handshake);
    }
}
