/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player.configuration;

import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import org.jetbrains.annotations.NotNull;

/**
 * This event is executed when a player has entered the configuration state.
 *
 * <p>From this moment on, until the {@link PlayerFinishedConfigurationEvent} is executed,
 * the {@linkplain Player#getProtocolState()} method is guaranteed
 * to return {@link ProtocolState#CONFIGURATION}.</p>
 *
 * @param player The player who has entered the configuration state.
 * @param server The server that will now (re-)configure the player.
 * @since 3.3.0
 * @sinceMinecraft 1.20.2
 */
public record PlayerEnteredConfigurationEvent(@NotNull Player player, ServerConnection server) {
}
