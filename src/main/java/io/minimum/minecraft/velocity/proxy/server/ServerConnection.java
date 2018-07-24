package io.minimum.minecraft.velocity.proxy.server;

import io.minimum.minecraft.velocity.protocol.StateRegistry;
import io.minimum.minecraft.velocity.proxy.ConnectedPlayer;
import io.netty.channel.Channel;

public class ServerConnection {
    private final Channel remoteServer;
    private final ConnectedPlayer proxyPlayer;
    private StateRegistry registry;

    public ServerConnection(Channel remoteServer, ConnectedPlayer proxyPlayer) {
        this.remoteServer = remoteServer;
        this.proxyPlayer = proxyPlayer;
        this.registry = StateRegistry.HANDSHAKE;
    }


}
