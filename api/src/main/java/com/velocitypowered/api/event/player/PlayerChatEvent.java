package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

/**
 * This event is fired when a player types in a chat message.
 */
public final class PlayerChatEvent implements ResultedEvent<PlayerChatEvent.ChatResult> {
    private final Player player;
    private final String message;
    private ChatResult result;

    public PlayerChatEvent(Player player, String message) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.message = Preconditions.checkNotNull(message, "message");
        this.result = ChatResult.allowed();
    }

    public Player getPlayer() {
        return player;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public ChatResult getResult() {
        return result;
    }

    @Override
    public void setResult(ChatResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }

    @Override
    public String toString() {
        return "PlayerChatEvent{" +
                "player=" + player +
                ", message=" + message +
                ", result=" + result +
                '}';
    }

    /**
     * Represents the result of the {@link PlayerChatEvent}.
     */
    public static final class ChatResult implements ResultedEvent.Result {
        private static final ChatResult ALLOWED = new ChatResult(true, null);
        private static final ChatResult DENIED = new ChatResult(false, null);

        // The server can not accept formatted text from clients!
        private @Nullable String message;
        private final boolean allowed;

        protected ChatResult(boolean allowed, @Nullable String message) {
            this.allowed = allowed;
            this.message = message;
        }

        @Override
        public boolean isAllowed() {
            return allowed;
        }

        @Override
        public String toString() {
            return allowed ? "allowed" : "denied";
        }

        public static ChatResult allowed() {
            return ALLOWED;
        }

        public static ChatResult denied() {
            return DENIED;
        }

        public Optional<String> getMessage() {
            return Optional.ofNullable(message);
        }

        public static ChatResult message(@NonNull String message) {
            Preconditions.checkNotNull(message, "message");
            return new ChatResult(true, message);
        }
    }


}
