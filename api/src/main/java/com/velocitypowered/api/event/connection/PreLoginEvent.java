package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;

import net.kyori.text.Component;

import java.util.Optional;

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
        private static final PreLoginComponentResult ALLOWED_ONLINEMODE = new PreLoginComponentResult(true, (GameProfile) null);

        private final boolean onlineMode;
        private final GameProfile gameProfile;
        /**
         * Allows to enable a online mode for the player connection, when velocity running in offline mode
         * Does not have any sense if velocity running in onlineMode;
         * @param onlineMode if true, offline uuid will be used for player connection to avoid collision
         */
        private PreLoginComponentResult(boolean onlineMode, GameProfile gameProfile) {
            super(true, null);
            this.onlineMode = onlineMode;
            this.gameProfile = gameProfile;
        }
        
        private PreLoginComponentResult(@Nullable Component reason) {
            super(reason == null, reason);
            // Don't care about this
            this.onlineMode = false;
            this.gameProfile = null;
        }

        public boolean isOnlineMode() {
            return this.onlineMode;
        }

        public Optional<GameProfile> getGameProfile() {
            return Optional.ofNullable(gameProfile);
        }
        
        @Override
        public String toString() {
            if (gameProfile != null && isOnlineMode()) {
                return "allowed with online mode and custom GameProfile";
            }
            if (isOnlineMode()) {
                return "allowed with online mode and offline uuid";
            }
            if (gameProfile != null) {
                return "allowed with custom GameProfile";
            }
            return super.toString();
        }

        public static PreLoginComponentResult allowed() {
            return ALLOWED;
        }

        public static PreLoginComponentResult allowedOnlineMode() {
            return ALLOWED_ONLINEMODE;
        }
        
        /**
         * Creates a PreLoginComponentResult with given arguments
         * @param onlineMode allows to enable a online mode for connection. False will be ignored if velocity running in online mode
         * @param gameProfile a GameProfile that will be used by player connection. Highly recommend to use offline uuid {@link GameProfile#createOfflineUUID(String)} if velocity running in offline mode
         * @return a PreLoginComponentResult with given arguments
         */
        public static PreLoginComponentResult create(boolean onlineMode, @NonNull GameProfile gameProfile) {
            Preconditions.checkNotNull(gameProfile, "profile");
            return new PreLoginComponentResult(onlineMode, gameProfile);
        }
        
        public static PreLoginComponentResult denied(@NonNull Component reason) {
            Preconditions.checkNotNull(reason, "reason");
            return new PreLoginComponentResult(reason);
        }
    }
}
