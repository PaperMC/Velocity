package io.minimum.minecraft.velocity.proxy.client;

import io.minimum.minecraft.velocity.protocol.MinecraftPacket;
import io.minimum.minecraft.velocity.proxy.MinecraftSessionHandler;

public class InitialConnectSessionHandler implements MinecraftSessionHandler {
    private final ConnectedPlayer player;

    public InitialConnectSessionHandler(ConnectedPlayer player) {
        this.player = player;
    }

    @Override
    public void handle(MinecraftPacket packet) {
        // No-op: will never handle packets
    }

    @Override
    public void disconnected() {
        // the user cancelled the login process
        player.getConnectedServer().disconnect();
    }
}
