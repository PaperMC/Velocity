package io.minimum.minecraft.velocity.proxy.handler;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.proxy.ConnectedPlayer;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;
import io.minimum.minecraft.velocity.proxy.ServerConnection;
import io.netty.buffer.ByteBuf;

public class PlaySessionHandler implements MinecraftSessionHandler {
    private final ConnectedPlayer player;
    private final ServerConnection connection;

    public PlaySessionHandler(ConnectedPlayer player, ServerConnection connection) {
        this.player = player;
        this.connection = connection;
    }

    @Override
    public void handle(MinecraftPacket packet) {

    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        connection.forward(buf);
    }
}
