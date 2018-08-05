package com.velocitypowered.api.proxy;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a Minecraft proxy server that follows the Velocity API.
 */
public interface ProxyServer {
    /**
     * Retrieves the player currently connected to this proxy by their Minecraft username.
     * @param username the username
     * @return an {@link Optional} with the player
     */
    Optional<Player> getPlayer(@Nonnull String username);

    /**
     * Retrieves the player currently connected to this proxy by their Minecraft UUID.
     * @param uuid the UUID
     * @return an {@link Optional} with the player
     */
    Optional<Player> getPlayer(@Nonnull UUID uuid);

    /**
     * Retrieves all players currently connected to this proxy. This call may or may not be a snapshot of all players
     * online.
     * @return the players online on this proxy
     */
    Collection<Player> getAllPlayers();
}
