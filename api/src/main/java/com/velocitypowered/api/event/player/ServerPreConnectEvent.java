package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.server.ServerInfo;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * This event is fired before the player connects to a server.
 */
public class ServerPreConnectEvent implements ResultedEvent<ServerPreConnectEvent.ServerResult> {
    private final Player player;
    private ServerResult result;

    public ServerPreConnectEvent(Player player, ServerResult result) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.result = Preconditions.checkNotNull(result, "result");
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public ServerResult getResult() {
        return result;
    }

    @Override
    public void setResult(@NonNull ServerResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    /**
     * Represents the result of the {@link ServerPreConnectEvent}.
     */
    public static class ServerResult implements ResultedEvent.Result {
        private static final ServerResult DENIED = new ServerResult(false, null);

        private final boolean allowed;
        private final ServerInfo info;

        private ServerResult(boolean allowed, @Nullable ServerInfo info) {
            this.allowed = allowed;
            this.info = info;
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        public Optional<ServerInfo> getInfo() {
            return Optional.ofNullable(info);
        }

        @Override
        public String toString() {
            if (!allowed) {
                return "denied";
            }
            return "allowed: connect to " + info.getName();
        }

        public static ServerResult denied() {
            return DENIED;
        }

        public static ServerResult allowed(ServerInfo server) {
            Preconditions.checkNotNull(server, "server");
            return new ServerResult(true, server);
        }
    }
}
