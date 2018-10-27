package com.velocitypowered.api.proxy.config;

import com.velocitypowered.api.util.Favicon;
import net.kyori.text.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Provides an interface to a proxy configuration
 */
public interface ProxyConfig {
    /**
     * Whether GameSpy 4 queries are accepted by the proxy
     * @return queries enabled
     */
    boolean isQueryEnabled();

    /**
     * Get the port GameSpy 4 queries are accepted on
     * @return the query port
     */
    int getQueryPort();

    /**
     * Get the map name reported to GameSpy 4 query services
     * @return the map name
     */
    String getQueryMap();

    /**
     * Whether GameSpy 4 queries should show plugins installed on
     * Velocity by default
     * @return show plugins in query
     */
    boolean shouldQueryShowPlugins();

    /**
     * Get the MOTD component shown in the tab list
     * @return the motd component
     */
    Component getMotdComponent();

    /**
     * Get the maximum players shown in the tab list
     * @return max players
     */
    int getShowMaxPlayers();

    /**
     * Get whether the proxy is online mode. This determines if players are authenticated with Mojang servers.
     * @return online mode enabled
     */
    boolean isOnlineMode();

    /**
     * Get a Map of all servers registered on this proxy
     * @return registered servers map
     */
    Map<String, String> getServers();

    /**
     * Get the order of servers that players will be connected to
     * @return connection order list
     */
    List<String> getAttemptConnectionOrder();

    /**
     * Get forced servers mapped to given virtual host
     * @return list of server names
     */
    Map<String, List<String>> getForcedHosts();

    /**
     * Get the minimum compression threshold for packets
     * @return the compression threshold
     */
    int getCompressionThreshold();

    /**
     * Get the level of compression that packets will be compressed to
     * @return the compression level
     */
    int getCompressionLevel();

    /**
     * Get the limit for how long a player must wait to log back in
     * @return the login rate limit (in milliseconds)
     */
    int getLoginRatelimit();

    /**
     * Get the proxy favicon shown in the tablist
     * @return optional favicon
     */
    Optional<Favicon> getFavicon();

    /**
     * Get whether this proxy displays that it supports Forge/FML
     * @return forge announce enabled
     */
    boolean isAnnounceForge();

    /**
     * Get how long this proxy will wait until performing a read timeout
     * @return connection timeout (in milliseconds)
     */
    int getConnectTimeout();

    /**
     * Get how long this proxy will wait until performing a read timeout
     * @return read timeout (in milliseconds)
     */
    int getReadTimeout();
}
