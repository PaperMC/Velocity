package com.velocitypowered.api.event.player.configuration;

import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;

// TODO: Protocol State API
/**
 * Event executed when a player of version 1.20.2 finishes the Configuration state.
 * <p>From this moment on, the {@link Player#protocolState()} method
 * will return {@link ProtocolState#PLAY}.</p>
 *
 * @param player The player who has completed the Configuration state
 */
public record PlayerFinishConfigurationEvent(@NotNull Player player) {
}
