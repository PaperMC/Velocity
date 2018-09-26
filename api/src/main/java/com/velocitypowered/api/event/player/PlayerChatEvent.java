package com.velocitypowered.api.event.player;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.proxy.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * This event is fired once the player has been authenticated but before they connect to a server on the proxy.
 */
public class PlayerChatEvent implements ResultedEvent<ResultedEvent.ChatResult> {
    private final Player player;
    private final String message;
    private ChatResult result;

    public PlayerChatEvent(Player player, String message) {
        this.player = Preconditions.checkNotNull(player, "player");
        this.message = Preconditions.checkNotNull(message, "message");
        this.result = (ChatResult) ChatResult.allowed();
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
    public void setResult(@NonNull ChatResult result) {
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


}
