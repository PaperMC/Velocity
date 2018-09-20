package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

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
     * Represents an "allowed/allowed with forced online\offline mode/denied" result with a reason allowed for denial.
     */
    public static class PreLoginComponentResult implements ResultedEvent.Result {

        private static final PreLoginComponentResult ALLOWED = new PreLoginComponentResult(Result.ALLOWED, null);
        private static final PreLoginComponentResult FORCE_ONLINEMODE = new PreLoginComponentResult(Result.FORCE_ONLINE, null);
        private static final PreLoginComponentResult FORCE_OFFLINEMODE = new PreLoginComponentResult(Result.FORCE_OFFLINE, null);

        private final Result result;
        private final Optional<Component> reason;

        private PreLoginComponentResult(Result result, @Nullable Component reason) {
            this.result = result;
            this.reason = Optional.ofNullable(reason);
        }

        @Override
        public boolean isAllowed() {
            return result != Result.DISALLOWED;
        }

        public Optional<Component> getReason() {
            return reason;
        }

        public boolean isOnlineModeAllowed() {
            return result == Result.FORCE_ONLINE;
        }

        public boolean isForceOfflineMode() {
            return result == Result.FORCE_OFFLINE;
        }

        @Override
        public String toString() {
            if (isForceOfflineMode()) {
                return "allowed with force offline mode";
            }
            if (isOnlineModeAllowed()) {
                return "allowed with online mode";
            }
            if (isAllowed()) {
                return "allowed";
            }
            if (reason.isPresent()) {
                return "denied: " + ComponentSerializers.PLAIN.serialize(reason.get());
            }
            return "denied";
        }

        /**
         * Returns a result indicating the connection will be allowed through
         * the proxy.
         * @return the allowed result
         */
        public static PreLoginComponentResult allowed() {
            return ALLOWED;
        }

        /**
         * Returns a result indicating the connection will be allowed through
         * the proxy, but the connection will be forced to use online mode
         * provided that the proxy is in offline mode. This acts similarly to
         * {@link #allowed()} on an online-mode proxy.
         * @return the result
         */
        public static PreLoginComponentResult forceOnlineMode() {
            return FORCE_ONLINEMODE;
        }

        /**
         * Returns a result indicating the connection will be allowed through
         * the proxy, but the connection will be forced to use offline mode even
         * when proxy running in online mode
         * @return the result
         */
        public static PreLoginComponentResult forceOfflineMode() {
            return FORCE_OFFLINEMODE;
        }

        /**
         * Denies the login with the specified reason.
         * @param reason the reason for disallowing the connection
         * @return a new result
         */
        public static PreLoginComponentResult denied(Component reason) {
            Preconditions.checkNotNull(reason, "reason");
            return new PreLoginComponentResult(Result.DISALLOWED, reason);
        }

        private enum Result {
            ALLOWED,
            FORCE_ONLINE,
            FORCE_OFFLINE,
            DISALLOWED
        }
    }
}
