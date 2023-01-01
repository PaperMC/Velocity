/*
 * Copyright (C) 2018-2021 Velocity Contributors
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

/**
 * Exposes certain proxy configuration information that plugins may use.
 */
public interface ProxyConfig {

  /**
   * Whether GameSpy 4 queries are accepted by the proxy.
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
  net.kyori.adventure.text.Component getMotd();

  /**
   * Get the maximum players shown in the tab list.
   *
   * @return max players
   */
  int getShowMaxPlayers();

  /**
   * Get whether the proxy is online mode. This determines if players are authenticated with Mojang.
   * servers.
   *
   * @return online mode enabled
   */
  boolean isOnlineMode();

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
   */
  Map<String, String> getServers();

  /**
   * Get the order of servers that players will be connected to.
   *
   * @return connection order list
   */
  List<String> getAttemptConnectionOrder();

  /**
   * Get forced servers mapped to a given virtual host.
   *
   * @return list of server names
   */
  Map<String, List<String>> getForcedHosts();

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
}
