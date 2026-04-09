/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.config;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.util.Favicon;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.kyori.adventure.text.Component;

/**
 * Exposes certain proxy configuration information that plugins may use.
 */
public interface ProxyConfig {

  /**
   * Whether the proxy accepts GameSpy 4 queries.
   *
   * @return queries enabled
   */
  boolean isQueryEnabled();

  /**
   * Get the port GameSpy 4 queries are accepted on.
   *
   * @return the query port
   */
  int getQueryPort();

  /**
   * Get the map name reported to GameSpy 4 query services.
   *
   * @return the map name
   */
  String getQueryMap();

  /**
   * Whether GameSpy 4 queries should show plugins installed on Velocity by default.
   *
   * @return show plugins in query
   */
  boolean shouldQueryShowPlugins();

  /**
   * Get the MOTD component shown in the tab list.
   *
   * @return the motd component
   */
  Component getMotd();

  /**
   * Get the MOTD hover legacy component shown in the tab list.
   *
   * @return the motd legacy component
   */
  List<Component> getMotdHover();

  /**
   * Get the maximum players shown in the tab list.
   *
   * @return max players
   */
  int getShowMaxPlayers();

  /**
   * Get whether the proxy is online mode.
   * This determines if players are authenticated with Mojang's
   * Authentication Servers.
   *
   * @return online mode enabled
   */
  boolean isOnlineMode();

  /**
   * Whether the proxy should tell client that proxy prevents chat reports, useful in NoChatReports mod. (1.19+).
   * 
   * @return does prevents chat reports
   */
  boolean doesPreventChatReports();

  /**
   * If client's ISP/AS sent from this proxy is different from the one from Mojang's
   * authentication server, the player is kicked. This disallows some VPN and proxy
   * connections but is a weak form of protection.
   *
   * @return whether to prevent client proxy connections by checking the IP with Mojang servers
   */
  boolean shouldPreventClientProxyConnections();

  /**
   * Get a Map of all servers registered in <code>velocity.toml</code>. This method does
   * <strong>not</strong> return all the servers currently in memory, although in most cases it
   * does. For a view of all registered servers, see {@link ProxyServer#getAllServers()}.
   *
   * @return registered servers map
   * @deprecated use {@link #getBackendServers()} instead.
   */
  @Deprecated(forRemoval = true, since = "3.4.0")
  Map<String, String> getServers();

  /**
   * Get a Map of all servers registered in <code>velocity.toml</code>. This method does
   * <strong>not</strong> return all the servers currently in memory, although in most cases it
   * does. For a view of all registered servers, see {@link ProxyServer#getAllServers()}.
   *
   * @return registered servers map with, instead of the only address, the Backend Server Object for each
   *     of them which contains the address of the server and its info forwarding mode.
   * @since 3.4.0
   * @see com.velocitypowered.api.proxy.server.PlayerInfoForwarding
   */
  Map<String, BackendServerConfig> getBackendServers();

  /**
   * Get the order of servers that players will be connected to.
   *
   * @return connection order list
   */
  List<String> getAttemptConnectionOrder();

  /**
   * Get forced servers mapped to a given virtual host.
   *
   * @return mapped list of server names
   */
  Map<String, List<String>> getForcedHosts();

  /**
   * Whether to use the forced hosts list as the fallback servers, instead of
   * servers-to-try ({@code getAttemptConnectionOrder}), if a forced host
   * is configured for the virtual host a player is connected to.
   *
   * @return true if {@code getForcedHosts().get(virtualHost)} should be used instead of {@code getAttemptConnectionOrder}.
   */
  boolean isForcedHostAsFallback();

  /**
   * Get the minimum compression threshold for packets.
   *
   * @return the compression threshold
   */
  int getCompressionThreshold();

  /**
   * Get the level of compression that packets will be compressed to.
   *
   * @return the compression level
   */
  int getCompressionLevel();

  /**
   * Get the limit for how long a player must wait to log back in.
   *
   * @return the login rate limit (in milliseconds)
   */
  int getLoginRatelimit();

  /**
   * Get the proxy favicon shown in the tablist.
   *
   * @return optional favicon
   */
  Optional<Favicon> getFavicon();

  /**
   * Get whether this proxy displays that it supports Forge/FML.
   *
   * @return forge announce enabled
   */
  boolean isAnnounceForge();

  /**
   * Get how long this proxy will wait for a connection to be established before timing it out.
   *
   * @return connection timeout (in milliseconds)
   */
  int getConnectTimeout();

  /**
   * Get how long this proxy will wait until performing a read timeout.
   *
   * @return read timeout (in milliseconds)
   */
  int getReadTimeout();

  /**
   * Get the rate limit for how fast a player can execute commands.
   *
   * @return the command rate limit (in milliseconds)
   */
  int getCommandRatelimit();

  /**
   * Get whether we should forward commands to the backend if the player is rate limited.
   *
   * @return whether to forward commands if rate-limited
   */
  boolean isForwardCommandsIfRateLimited();

  /**
   * Get the kick limit for commands that are rate limited.
   * If this limit is 0 or less, the player will not be kicked.
   *
   * @return the rate-limited command rate limit
   */
  int getKickAfterRateLimitedCommands();

  /**
   * Get whether the proxy should kick players who are command rate-limited.
   *
   * @return whether to kick players who are rate limited
   */
  default boolean isKickOnCommandRateLimit() {
    return getKickAfterRateLimitedCommands() > 0;
  }

  /**
   * Get the rate limit for how fast a player can tab complete.
   *
   * @return the tab complete rate limit (in milliseconds)
   */
  int getTabCompleteRatelimit();

  /**
   * Get the kick limit for tab completes that are rate limited.
   * If this limit is 0 or less, the player will not be kicked.
   *
   * @return the rate-limited command rate limit
   */
  int getKickAfterRateLimitedTabCompletes();

  /**
   * Get whether the proxy should kick players who are tab complete rate limited.
   *
   * @return whether to kick players who are rate limited
   */
  default boolean isKickOnTabCompleteRateLimit() {
    return getKickAfterRateLimitedTabCompletes() > 0;
  }
}
