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
 * This event is executed when a player is about to finish the configuration state.
 *
 * <p>Velocity will wait for this event before asking the client to finish the configuration state.
 * However, due to backend server being unable to keep the connection alive during state changes,
 * Velocity will only wait for a maximum of 5 seconds. If you need to hold a player in configuration
 * state, use the {@link PlayerConfigurationEvent}.</p>
 *
 * @param player The player who is about to finish the configuration phase.
 * @param server The server that has (re-)configured the player.
 * @since 3.3.0
 * @sinceMinecraft 1.20.2
 */
@AwaitingEvent
public record PlayerFinishConfigurationEvent(@NotNull Player player, @NotNull ServerConnection server) {
}
