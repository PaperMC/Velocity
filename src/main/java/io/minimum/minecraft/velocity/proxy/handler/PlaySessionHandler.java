package io.minimum.minecraft.velocity.proxy.handler;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.Chat;
import io.minimum.minecraft.velocity.protocol.packets.Ping;
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
        if (packet instanceof Ping) {
            // Handle the ping.
            player.getConnection().write(packet);
            return;
        }

        if (packet instanceof Chat) {
            // TODO: handle this ourselves, for now do this
            player.getConnectedServer().forward(packet);
        }
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        connection.forward(buf.retain());
    }
}
