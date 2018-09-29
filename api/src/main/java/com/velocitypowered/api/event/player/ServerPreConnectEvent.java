package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * This event is fired before the player connects to a server.
 */
public final class ServerPreConnectEvent implements ResultedEvent<ServerPreConnectEvent.ServerResult> {
    private final Player player;
    private final RegisteredServer originalServer;
    private ServerResult result;

    public ServerPreConnectEvent(Player player, RegisteredServer originalServer) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.originalServer = Preconditions.checkNotNull(originalServer, "originalServer");
        this.result = ServerResult.allowed(originalServer);
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

    public RegisteredServer getOriginalServer() {
        return originalServer;
    }

    @Override
    public String toString() {
        return "ServerPreConnectEvent{" +
                "player=" + player +
                ", originalServer=" + originalServer +
                ", result=" + result +
                '}';
    }

    /**
     * Represents the result of the {@link ServerPreConnectEvent}.
     */
    public static class ServerResult implements ResultedEvent.Result {
        private static final ServerResult DENIED = new ServerResult(false, null);

        private final boolean allowed;
        private final RegisteredServer server;

        private ServerResult(boolean allowed, @Nullable RegisteredServer server) {
            this.allowed = allowed;
            this.server = server;
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        public Optional<RegisteredServer> getServer() {
            return Optional.ofNullable(server);
        }

        @Override
        public String toString() {
            if (!allowed) {
                return "denied";
            }
            return "allowed: connect to " + server.getServerInfo().getName();
        }

        public static ServerResult denied() {
            return DENIED;
        }

        public static ServerResult allowed(RegisteredServer server) {
            Preconditions.checkNotNull(server, "server");
            return new ServerResult(true, server);
        }
    }
}
