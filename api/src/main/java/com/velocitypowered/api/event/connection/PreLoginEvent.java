package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.proxy.InboundConnection;

import net.kyori.text.Component;


import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a player has initiated a connection with the proxy but before the proxy authenticates the
 * player with Mojang or before the player's proxy connection is fully established (for offline mode).
 */
public class PreLoginEvent implements ResultedEvent<PreLoginEvent.PreLoginComponentResult> {
    private final InboundConnection connection;
    private final String username;
    private PreLoginComponentResult result;

    public PreLoginEvent(InboundConnection connection, String username) {
        this.connection = Preconditions.checkNotNull(connection, "connection");
        this.username = Preconditions.checkNotNull(username, "username");
        this.result = PreLoginComponentResult.allowed();
    }

    public InboundConnection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public PreLoginComponentResult getResult() {
        return result;
    }

    @Override
    public void setResult(@NonNull PreLoginComponentResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    @Override
    public String toString() {
        return "PreLoginEvent{" +
                "connection=" + connection +
                ", username='" + username + '\'' +
                ", result=" + result +
                '}';
    }

    /**
     * Represents an "allowed/allowed with online mode/denied" result with a reason allowed for denial.
     */
    public static class PreLoginComponentResult extends ComponentResult {

        private static final PreLoginComponentResult ALLOWED = new PreLoginComponentResult((Component) null);
        private static final PreLoginComponentResult FORCE_ONLINEMODE = new PreLoginComponentResult(true);

        private final boolean onlineMode;
        /**
         * Allows to enable a online mode for the player connection, when Velocity running in offline mode
         * Does not have any sense if velocity running in onlineMode;
         * @param allowedOnlineMode if true, offline uuid will be used for player connection if Velocity run in offlineMode
         */
        private PreLoginComponentResult(boolean allowedOnlineMode) {
            super(true, null);
            this.onlineMode = allowedOnlineMode;
        }

        private PreLoginComponentResult(@Nullable Component reason) {
            super(reason == null, reason);
            // Don't care about this
            this.onlineMode = false;
        }

        public boolean isOnlineModeAllowed() {
            return this.onlineMode;
        }

        @Override
        public String toString() {
            if (isOnlineModeAllowed()) {
                return "allowed with online mode";
            }
            
            return super.toString();
        }

        public static PreLoginComponentResult allowed() {
            return ALLOWED;
        }

        public static PreLoginComponentResult forceOnlineMode() {
            return FORCE_ONLINEMODE;
        }

        public static PreLoginComponentResult denied(@NonNull Component reason) {
            Preconditions.checkNotNull(reason, "reason");
            return new PreLoginComponentResult(reason);
        }
    }
}
