package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * This event is fired when the server tries to authenticate the player as online mode.
 */
@AwaitingEvent
public final class AuthenticationEvent implements ResultedEvent<AuthenticationEvent.AuthComponentResult> {

    private AuthComponentResult result;
    private final InboundConnection connection;
    private final String username;
    private final UUID uniqueId;
    private GameProfile profile;
    private boolean allowed;

    public AuthenticationEvent(InboundConnection connection, String username, UUID uniqueId) {
        this.connection = connection;
        this.username = username;
        this.uniqueId = uniqueId;
    }

    public InboundConnection getConnection() {
        return connection;
    }

    public String getUsername() {
        return username;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public void setProfile(GameProfile profile) {
        this.profile = profile;
    }

    public GameProfile getProfile() {
        return profile;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    @Override
    public AuthComponentResult getResult() {
        return result;
    }

    @Override
    public void setResult(AuthComponentResult result) {
        this.result = result;

        allowed = result.isAllowed();
    }

    public static final class AuthComponentResult implements ResultedEvent.Result {

        private static final AuthComponentResult
           AUTHENTICATED = new AuthComponentResult(Result.AUTHENTICATED, null),
           NOT_AUTHENTICATED = new AuthComponentResult(Result.NOT_AUTHENTICATED, null),
           API_SERVERS_DOWN = new AuthComponentResult(Result.API_SERVERS_DOWN, null),
           GENERIC_ERROR = new AuthComponentResult(Result.GENERIC_ERROR, null),
           INVALID_PUBLIC_KEY = new AuthComponentResult(Result.INVALID_PUBLIC_KEY, null);

        private final Result result;
        private final Component reason;

        private AuthComponentResult(Result result,
                                    @Nullable Component reason) {
            this.result = result;
            this.reason = reason;
        }

        public Result toEnum() {
            return result;
        }

        @Override
        public boolean isAllowed() {
            return result == Result.AUTHENTICATED;
        }

        public Optional<Component> getReasonComponent() {
            return Optional.ofNullable(reason);
        }

        @Override
        public String toString() {
            return switch (result) {
                case AUTHENTICATED -> "allowed";
                case NOT_AUTHENTICATED -> "disallowed, connection is offline-mode";
                case API_SERVERS_DOWN -> "couldn't verify connection authentication";
                case GENERIC_ERROR -> "internal error has occurred";
                case INVALID_PUBLIC_KEY -> "public key is not valid, uuid spoof?";
            };
        }

        public static AuthComponentResult authenticated() {
            return AUTHENTICATED;
        }

        public static AuthComponentResult notAuthenticated() {
            return NOT_AUTHENTICATED;
        }

        public static AuthComponentResult apiServersDown() {
            return API_SERVERS_DOWN;
        }

        public static AuthComponentResult genericError() {
            return GENERIC_ERROR;
        }

        public static AuthComponentResult invalidPublicKey() {
            return INVALID_PUBLIC_KEY;
        }

        public enum Result {

            AUTHENTICATED,
            NOT_AUTHENTICATED,
            API_SERVERS_DOWN,
            GENERIC_ERROR,
            INVALID_PUBLIC_KEY

        }
    }

}