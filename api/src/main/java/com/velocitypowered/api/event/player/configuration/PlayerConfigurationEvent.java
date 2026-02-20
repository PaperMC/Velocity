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
 * This event is executed when a player entered the configuration state and can be configured by Velocity.
 *
 * <p>Velocity will wait for this event before continuing/ending the configuration state.</p>
 *
 * @param player The player who can be configured.
 * @param server The server that is currently configuring the player.
 * @since 3.3.0
 * @sinceMinecraft 1.20.2
 */
@AwaitingEvent
public record PlayerConfigurationEvent(@NotNull Player player, ServerConnection server) {
}
