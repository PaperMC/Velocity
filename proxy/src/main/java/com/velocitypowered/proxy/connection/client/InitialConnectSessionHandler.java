package com.velocitypowered.proxy.connection.client;

import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.MinecraftPacket;

public class InitialConnectSessionHandler implements MinecraftSessionHandler {

    private final ConnectedPlayer player;

    public InitialConnectSessionHandler(ConnectedPlayer player) {
        this.player = player;
    }

    @Override
    public PacketStatus handle(MinecraftPacket packet) {
        // No-op: will never handle packets
        return PacketStatus.CANCEL;
    }

    @Override
    public void disconnected() {
        // the user cancelled the login process
        player.teardown();
    }
}
