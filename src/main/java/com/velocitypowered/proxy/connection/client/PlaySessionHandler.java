package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.protocol.MinecraftPacket;
import com.velocitypowered.proxy.protocol.packets.Ping;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import io.netty.buffer.ByteBuf;

public class PlaySessionHandler implements MinecraftSessionHandler {
    private final ConnectedPlayer player;

    public PlaySessionHandler(ConnectedPlayer player) {
        this.player = player;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        if (packet instanceof Ping) {
            // Handle the ping.
            player.getConnection().write(packet);
            return;
        }

        // If we don't want to handle this packet, just forward it on.
        player.getConnectedServer().getChannel().write(packet);
    }

    @Override
    public void handleUnknown(ByteBuf buf) {
        player.getConnectedServer().getChannel().write(buf.retain());
    }

    @Override
    public void disconnected() {
        player.getConnectedServer().disconnect();
    }
}
