/*
 * Copyright (C) 2019-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;

/**
 * For clients 1.12.2 and below, this event is triggered when the player
 * requests a Tab complete from the remote server. Velocity will wait for this
 * event to complete, and then decide what information to send to the remote
 * server based on the result of the trigger.
 *
 * @since 3.3.0
 */
@AwaitingEvent
public class PlayerTabCompleteEvent implements ResultedEvent<ResultedEvent.GenericResult> {

    private final Player player;
    private String partialMessage;
    private GenericResult result;

    /**
     * Constructs a new TabCompleteEvent instance.
     *
     * @param player the player
     * @param partialMessage the partial message
     */
    public PlayerTabCompleteEvent(Player player, String partialMessage) {
        this.player = checkNotNull(player, "player");
        this.partialMessage = checkNotNull(partialMessage, "partialMessage");
    }

    /**
     * Returns the player requesting the tab completion.
     *
     * @return the requesting player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the message being partially completed.
     *
     * @return the partial message
     */
    public String getPartialMessage() {
        return partialMessage;
    }

    /**
     * Set the message being partially completed.
     */
    public void setPartialMessage(String partialMessage) {
        this.partialMessage = Preconditions.checkNotNull(partialMessage, "partialMessage");
    }

    @Override
    public String toString() {
        return "PlayerTabCompleteEvent{"
            + "player=" + player
            + ", partialMessage='" + partialMessage + '\''
            + ", result=" + result
            + '}';
    }

    @Override
    public GenericResult getResult() {
        return result;
    }

    @Override
    public void setResult(GenericResult result) {
        this.result = Preconditions.checkNotNull(result, "result");
    }
}
