/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player.configuration;

import com.velocitypowered.api.event.annotation.AwaitingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import org.jetbrains.annotations.NotNull;

/**
 * This event is executed when the player is about to finish the Configuration state.
 * <p>Velocity will wait for this event to finish the configuration phase on the client.</p>
 *
 * @param player The player who is about to complete the configuration phase.
 * @param server The server that is currently (re-)configuring the player.
 * @since 3.3.0
 * @sinceMinecraft 1.20.2
 */
@AwaitingEvent
public record PlayerFinishConfigurationEvent(@NotNull Player player, @NotNull ServerConnection server) {
}
