package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;

/**
 * This event is fired once the player has successfully connected to the target server and the connection to the previous
 * server has been de-established.
 */
public class ServerConnectedEvent {
    private final Player player;
    private final ServerInfo server;

    public ServerConnectedEvent(Player player, ServerInfo server) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.server = Preconditions.checkNotNull(server, "server");
    }

    public Player getPlayer() {
        return player;
    }

    public ServerInfo getServer() {
        return server;
    }

    @Override
    public String toString() {
        return "ServerConnectedEvent{" +
                "player=" + player +
                ", server=" + server +
                '}';
    }
}
