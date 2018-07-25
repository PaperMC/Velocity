package io.minimum.minecraft.velocity.proxy.client;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.protocol.packets.Chat;
import io.minimum.minecraft.velocity.protocol.packets.Ping;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;
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
