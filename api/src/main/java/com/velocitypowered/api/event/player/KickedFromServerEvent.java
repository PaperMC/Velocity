package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Fired when a player is kicked from a server. You may either allow Velocity to kick the player (with an optional reason
 * override) or redirect the player to a separate server.
 */
public class KickedFromServerEvent implements ResultedEvent<KickedFromServerEvent.ServerKickResult> {
    private final Player player;
    private final ServerInfo server;
    private final Component originalReason;
    private final boolean duringLogin;
    private ServerKickResult result;

    public KickedFromServerEvent(Player player, ServerInfo server, Component originalReason, boolean duringLogin, Component fancyReason) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.server = Preconditions.checkNotNull(server, "server");
        this.originalReason = Preconditions.checkNotNull(originalReason, "originalReason");
        this.duringLogin = duringLogin;
        this.result = new DisconnectPlayer(fancyReason);
    }

    @Override
    public ServerKickResult getResult() {
        return result;
    }

    @Override
    public void setResult(@NonNull ServerKickResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    public Player getPlayer() {
        return player;
    }

    public ServerInfo getServer() {
        return server;
    }

    public Component getOriginalReason() {
        return originalReason;
    }

    public boolean kickedDuringLogin() {
        return duringLogin;
    }

    public interface ServerKickResult extends ResultedEvent.Result {}

    public static class DisconnectPlayer implements ServerKickResult {
        private final Component component;

        private DisconnectPlayer(Component component) {
            this.component = Preconditions.checkNotNull(component, "component");
        }

        @Override
        public boolean isAllowed() {
            return true;
        }

        public Component getReason() {
            return component;
        }

        public static DisconnectPlayer create(Component component) {
            return new DisconnectPlayer(component);
        }
    }

    public static class RedirectPlayer implements ServerKickResult {
        private final ServerInfo server;

        private RedirectPlayer(ServerInfo server) {
            this.server = Preconditions.checkNotNull(server, "server");
        }

        @Override
        public boolean isAllowed() {
            return false;
        }

        public ServerInfo getServer() {
            return server;
        }

        public static RedirectPlayer create(ServerInfo info) {
            return new RedirectPlayer(info);
        }
    }
}
