/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.player.configuration;

import com.velocitypowered.api.network.ProtocolState;
import com.velocitypowered.api.proxy.Player;

/**
 * Event executed when a player of version 1.20.2 or higher finishes the Configuration state.
 * <p>From this moment on, the {@link Player#getProtocolState()} method
 * will return {@link ProtocolState#PLAY}.</p>
 *
 * @param player The player who has completed the Configuration state
 * @since 3.3.0
 * @sinceMinecraft 1.20.2
 */
public record PlayerFinishedConfigurationEvent(Player player) {
}
