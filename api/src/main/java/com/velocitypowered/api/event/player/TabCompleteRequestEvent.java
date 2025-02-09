/*
 * Copyright (C) 2019-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player;

import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This event is fired after a tab complete request is sent by the player, for clients on
 * 1.12.2 and below. You have the opportunity to modify the request sent to the remote server.
 */
@AwaitingEvent
public class TabCompleteRequestEvent {

    private final Player player;
    private String partialMessage;

    /**
     * Constructs a new TabCompleteRequestEvent instance.
     *
     * @param player the player
     * @param partialMessage the partial message
     */
    public TabCompleteRequestEvent(Player player, String partialMessage) {
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
     * Modifies the message being partially completed.
     *
     */
    public void setPartialMessage(String partialMessage) {
        this.partialMessage = partialMessage;
    }

    @Override
    public String toString() {
        return "TabCompleteRequestEvent{"
                + "player=" + player
                + ", partialMessage='" + partialMessage + '\''
                + '}';
    }
}
