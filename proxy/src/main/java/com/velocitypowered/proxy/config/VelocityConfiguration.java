/*
 * Copyright (C) 2018-2026 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.Expose;
import com.velocityctd.proxy.config.migration.CtdConfigMigrations;
import com.velocitypowered.api.proxy.config.BackendServerConfig;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.proxy.server.ServerInfoForwardingMode;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ServerLink;
import com.velocitypowered.proxy.config.migration.ConfigurationMigration;
import com.velocitypowered.proxy.config.migration.ForwardingMigration;
import com.velocitypowered.proxy.config.migration.KeyAuthenticationMigration;
import com.velocitypowered.proxy.config.migration.MiniMessageTranslationsMigration;
import com.velocitypowered.proxy.config.migration.MotdMigration;
import com.velocitypowered.proxy.config.migration.TransferIntegrationMigration;
import com.velocitypowered.proxy.util.AddressUtil;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * Velocity's configuration.
 */
@SuppressWarnings("unchecked")
public final class VelocityConfiguration implements ProxyConfig {

  /**
   * The logger used to print configuration-related warnings and errors.
   */
  private static final Logger LOGGER = LogManager.getLogger(VelocityConfiguration.class);

  private static final String UNBOUNDED = "UNBOUNDED";

  /**
   * The IP address and port the proxy binds to.
   * Format: {@code ip:port}, e.g., {@code 0.0.0.0:25565}.
   */
  @Expose
  private String bind = "0.0.0.0:25565";

  /**
   * The Message of the Day (MOTD) shown to clients in the server list.
   */
  @Expose
  private String motd = "<aqua>A Velocity Server";

  /**
   * Parsed MiniMessage component version of the MOTD, lazily initialized.
   */
  private @MonotonicNonNull Component motdAsComponent;

  /**
   * The hover text shown when a user hovers over the MOTD in the server list.
   */
  @Expose
  private List<String> motdHover = List.of("");

  /**
   * Parsed hover components from {@link #motdHover}, lazily initialized.
   */
  private List<@MonotonicNonNull Component> motdHoverComponents;

  /**
   * The maximum number of players shown to the client in the server list ping.
   */
  @Expose
  private int showMaxPlayers = 500;

  /**
   * Whether the proxy should attempt to verify Mojang authentication.
   */
  @Expose
  private boolean onlineMode = true;

  /**
   * Whether to prevent clients from connecting directly to backend servers.
   */
  @Expose
  private boolean preventClientProxyConnections = false;

  /**
   * Global player info forwarding strategy used by the proxy.
   */
  @Expose
  private PlayerInfoForwarding playerInfoForwardingMode = PlayerInfoForwarding.NONE;

  /**
   * Shared secret used to validate forwarded player info (Modern/BungeeGuard).
   */
  private byte[] forwardingSecret = generateRandomString(12).getBytes(StandardCharsets.UTF_8);

  /**
   * Whether to announce Forge support to clients in the handshake.
   */
  @Expose
  private boolean announceForge = false;

  /**
   * If {@code true}, when a new connection arrives with the same UUID as an already-online player,
   * the existing session is kicked and the new connection is allowed in. Works in both online and
   * offline mode.
   */
  @Expose
  private boolean kickExistingPlayers = false;

  /**
   * If {@code true}, kick-existing-players will only kick the existing session when the new
   * connection originates from the same IP address. Connections from a different IP with a
   * duplicate UUID are denied instead of displacing the existing player.
   */
  @Expose
  private boolean kickExistingPlayersCheckIp = false;

  /**
   * Defines how ping data (e.g. MOTD, players, mods) is forwarded to clients.
   */
  @Expose
  private PingPassthroughMode pingPassthrough = PingPassthroughMode.DISABLED;

  /**
   * Whether to include actual player samples in ping response (e.g. tab previews).
   */
  @Expose
  private boolean samplePlayersInPing = false;

  /**
   * The configured backend servers and their forwarding behavior.
   */
  private final Servers servers;

  /**
   * Virtual host mappings that reroute players to specific server lists.
   */
  private final ForcedHosts forcedHosts;

  /**
   * Controls which built-in proxy commands are enabled.
   */
  @Expose
  private final Commands commands;

  /**
   * Maps command aliases to their underlying command paths.
   */
  @Expose
  private final CommandAliases commandAliases;

  /**
   * Maps proxy command aliases to their underlying command executions.
   * These are new commands that execute other commands when invoked.
   */
  @Expose
  private final ProxyCommandAliases proxyCommandAliases;

  /**
   * Advanced configuration options for performance and features.
   */
  @Expose
  private final Advanced advanced;

  /**
   * Query protocol support configuration.
   */
  @Expose
  private final Query query;

  /**
   * Metrics configuration for enabling or disabling bStats.
   */
  private final Metrics metrics;

  /**
   * Redis configuration used for multi-proxy functionality.
   */
  @Expose
  private final Redis redis;

  /**
   * Queue configuration used for handling players attempting to connect to servers.
   */
  @Expose
  private final Queue queue;

  /**
   * Whether to log each player's IP address during connection.
   */
  @Expose
  private boolean enablePlayerAddressLogging = true;

  /**
   * Optional favicon shown in ping response. May be null.
   */
  private @Nullable Favicon favicon;

  /**
   * Whether key authentication is enforced when online-mode is enabled.
   * Was added in Minecraft 1.19.
   */
  @Expose
  private boolean forceKeyAuthentication = true;

  /**
   * Whether to log all player connection attempts.
   */
  @Expose
  private boolean logPlayerConnections = true;

  /**
   * Whether to log all player disconnections.
   */
  @Expose
  private boolean logPlayerDisconnections = true;

  /**
   * Whether to log connections that fail authentication or timeout.
   */
  @Expose
  private boolean logOfflineConnections = true;

  /**
   * Whether to disable Forge negotiation and related plugin messages.
   */
  @Expose
  private boolean disableForge = false;

  /**
   * Whether to enforce that clients use Mojang's chat signing mechanism.
   */
  @Expose
  private boolean enforceChatSigning = true;

  /**
   * Whether the proxy should tell client that proxy prevents chat reports, useful in NoChatReports mod. (1.19+).
   */
  @Expose
  private boolean preventsChatReports = false;

  /**
   * Whether to translate MiniMessage headers and footers into legacy color codes.
   */
  @Expose
  private boolean translateHeaderFooter = true;

  /**
   * Whether to log minimum supported client version in console.
   */
  @Expose
  private boolean logMinimumVersion = false;

  /**
   * The lowest allowed Minecraft client version that can connect to the proxy.
   */
  @Expose
  private String minimumVersion = "1.7.2";

  /**
   * The highest allowed Minecraft client version that can connect to the proxy.
   * Set to "UNBOUNDED" to allow any version up to the protocol maximum.
   */
  @Expose
  private String maximumVersion = UNBOUNDED;

  /**
   * Slash-command shortcuts for routing players to specific servers.
   */
  @Expose
  private Map<String, List<String>> slashServers = new HashMap<>();

  /**
   * A list of configured links available to players via server menus.
   */
  @Expose
  private Map<String, List<ServerLink>> serverLinks = new HashMap<>();

  /**
   * A list of configured proxy instances, used in multi-proxy setups.
   */
  @Expose
  private List<ProxyAddress> proxyAddresses = new ArrayList<>();

  /**
   * Filter strategy used to select the best proxy from {@link #proxyAddresses}.
   */
  @Expose
  private DynamicProxyFilterMode dynamicProxyFilter;

  /**
   * Server-specific player cap overrides (used for dynamic balancing).
   */
  @Expose
  private Map<String, Integer> playerCaps;

  private VelocityConfiguration(final String bind, final String motd, final List<String> motdHover,
                                final int showMaxPlayers, final boolean onlineMode,
                                final boolean preventClientProxyConnections, final boolean announceForge,
                                final PlayerInfoForwarding playerInfoForwardingMode, final byte[] forwardingSecret,
                                final boolean kickExistingPlayers, final boolean kickExistingPlayersCheckIp,
                                final PingPassthroughMode pingPassthrough,
                                final boolean samplePlayersInPing, final boolean enablePlayerAddressLogging,
                                final Servers servers, final ForcedHosts forcedHosts, final CommandAliases commandAliases,
                                final ProxyCommandAliases proxyCommandAliases, final Commands commands, final Advanced advanced,
                                final Query query, final Metrics metrics, final boolean forceKeyAuthentication,
                                final boolean logPlayerConnections, final boolean logPlayerDisconnections,
                                final boolean logOfflineConnections, final boolean disableForge,
                                final boolean enforceChatSigning, final boolean preventsChatReports, final boolean translateHeaderFooter,
                                final boolean logMinimumVersion, final String minimumVersion,
                                final String maximumVersion,
                                final Redis redis, final Queue queue, final Map<String, List<String>> slashServers,
                                final Map<String, List<ServerLink>> serverLinks, final List<ProxyAddress> proxyAddresses,
                                final DynamicProxyFilterMode dynamicProxyFilter, final Map<String, Integer> playerCaps) {
    this.bind = bind;
    this.motd = motd;
    this.motdHover = motdHover;
    this.showMaxPlayers = showMaxPlayers;
    this.onlineMode = onlineMode;
    this.preventClientProxyConnections = preventClientProxyConnections;
    this.announceForge = announceForge;
    this.playerInfoForwardingMode = playerInfoForwardingMode;
    this.forwardingSecret = forwardingSecret;
    this.kickExistingPlayers = kickExistingPlayers;
    this.kickExistingPlayersCheckIp = kickExistingPlayersCheckIp;
    this.pingPassthrough = pingPassthrough;
    this.samplePlayersInPing = samplePlayersInPing;
    this.enablePlayerAddressLogging = enablePlayerAddressLogging;
    this.servers = servers;
    this.forcedHosts = forcedHosts;
    this.commandAliases = commandAliases;
    this.proxyCommandAliases = proxyCommandAliases;
    this.commands = commands;
    this.advanced = advanced;
    this.query = query;
    this.metrics = metrics;
    this.forceKeyAuthentication = forceKeyAuthentication;
    this.logPlayerConnections = logPlayerConnections;
    this.logPlayerDisconnections = logPlayerDisconnections;
    this.logOfflineConnections = logOfflineConnections;
    this.disableForge = disableForge;
    this.enforceChatSigning = enforceChatSigning;
    this.preventsChatReports = preventsChatReports;
    this.translateHeaderFooter = translateHeaderFooter;
    this.logMinimumVersion = logMinimumVersion;
    this.minimumVersion = minimumVersion;
    this.maximumVersion = maximumVersion;
    this.redis = redis;
    this.queue = queue;
    this.slashServers = slashServers;
    this.serverLinks = serverLinks;
    this.proxyAddresses = proxyAddresses;
    this.dynamicProxyFilter = dynamicProxyFilter;
    this.playerCaps = playerCaps;
  }

  /**
   * Attempts to validate the configuration.
   *
   * @return {@code true} if the configuration is sound, {@code false} if not
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean validate() {
    boolean valid = true;

    if (bind.isEmpty()) {
      LOGGER.error("'bind' option is empty.");
      valid = false;
    } else {
      try {
        AddressUtil.parseAddress(bind);
      } catch (IllegalArgumentException e) {
        LOGGER.error("'bind' option does not specify a valid IP address.", e);
        valid = false;
      }
    }

    if (!onlineMode) {
      LOGGER.warn("The proxy is running in offline mode! This is a security risk and you will NOT "
          + "receive any support!");
    }

    switch (playerInfoForwardingMode) {
      case NONE -> LOGGER.warn("Player info forwarding is disabled! All players will appear to be connecting "
              + "from the proxy and will have offline-mode UUIDs.");
      case MODERN, BUNGEEGUARD -> {
        if (forwardingSecret == null || forwardingSecret.length == 0) {
          LOGGER.error("You don't have a forwarding secret set. This is required for security.");
          valid = false;
        }
      }
      default -> {
      }
    }

    if (servers.getBackendServers().isEmpty()) {
      LOGGER.warn("You don't have any servers configured.");
    }

    for (Map.Entry<String, BackendServerConfig> entry : servers.getBackendServers().entrySet()) {
      try {
        AddressUtil.parseAddress(entry.getValue().address());
      } catch (IllegalArgumentException e) {
        LOGGER.error("Server {} does not have a valid IP address.", entry.getKey(), e);
        valid = false;
      }

      ServerInfoForwardingMode mode = entry.getValue().forwardingMode();
      if (mode == ServerInfoForwardingMode.MODERN || mode == ServerInfoForwardingMode.BUNGEEGUARD) {
        if (forwardingSecret == null || forwardingSecret.length == 0) {
          LOGGER.error("You don't have a forwarding secret set. This is required if "
                  + "you are using MODERN or BUNGEEGUARD forwarding modes.");
          valid = false;
        }
      }
    }

    for (String s : servers.getAttemptConnectionOrder()) {
      if (!servers.getBackendServers().containsKey(s)) {
        LOGGER.error("Fallback server {} is not registered in your configuration!", s);
        valid = false;
      }
    }

    final Map<String, List<String>> configuredForcedHosts = forcedHosts.getForcedHosts();
    if (!configuredForcedHosts.isEmpty()) {
      for (Map.Entry<String, List<String>> entry : configuredForcedHosts.entrySet()) {
        if (entry.getValue().isEmpty()) {
          LOGGER.error("Forced host '{}' does not contain any servers", entry.getKey());
          valid = false;
          continue;
        }

        for (String server : entry.getValue()) {
          if (!servers.getBackendServers().containsKey(server)) {
            LOGGER.error("Server '{}' for forced host '{}' does not exist", server, entry.getKey());
            valid = false;
          }
        }
      }
    }

    for (Map.Entry<String, List<String>> entry : slashServers.entrySet()) {
      if (entry.getValue().isEmpty()) {
        LOGGER.error("Slash server alias '{}' does not contain any servers", entry.getKey());
        valid = false;
        continue;
      }

      if (!servers.getBackendServers().containsKey(entry.getKey())) {
        LOGGER.error("Server '{}' does not exist in slash server aliases", entry.getKey());
        valid = false;
      }
    }

    try {
      getMotd();
    } catch (Exception e) {
      LOGGER.error("Can't parse your MOTD", e);
      valid = false;
    }

    try {
      getMotdHover();
    } catch (Exception e) {
      LOGGER.error("Can't parse your MOTD hover", e);
      valid = false;
    }

    if (advanced.compressionLevel < -1 || advanced.compressionLevel > 9) {
      LOGGER.error("Invalid compression level {}", advanced.compressionLevel);
      valid = false;
    } else if (advanced.compressionLevel == 0) {
      LOGGER.warn("ALL packets going through the proxy will be uncompressed. This will increase "
          + "bandwidth usage.");
    }

    if (advanced.compressionThreshold < -1) {
      LOGGER.error("Invalid compression threshold {}", advanced.compressionLevel);
      valid = false;
    } else if (advanced.compressionThreshold == 0) {
      LOGGER.warn("ALL packets going through the proxy will be compressed. This will compromise "
          + "throughput and increase CPU usage!");
    }

    if (advanced.loginRatelimit < 0) {
      LOGGER.error("Invalid login ratelimit {}ms", advanced.loginRatelimit);
      valid = false;
    }

    if (advanced.commandRateLimit < 0) {
      LOGGER.error("Invalid command rate limit {}", advanced.commandRateLimit);
      valid = false;
    }

    loadFavicon();

    return valid;
  }

  private void loadFavicon() {
    Path faviconPath = Path.of("server-icon.png");
    if (Files.exists(faviconPath)) {
      try {
        this.favicon = Favicon.create(faviconPath);
      } catch (Exception e) {
        LOGGER.info("Unable to load your server-icon.png, continuing without it.", e);
      }
    }
  }

  /**
   * The current IP and port the proxy is bound to.
   *
   * @return the resolved bind address
   */
  public InetSocketAddress getBind() {
    return AddressUtil.parseAndResolveAddress(bind);
  }

  @Override
  public boolean isQueryEnabled() {
    return query.isQueryEnabled();
  }

  @Override
  public int getQueryPort() {
    return query.getQueryPort();
  }

  @Override
  public String getQueryMap() {
    return query.getQueryMap();
  }

  @Override
  public boolean shouldQueryShowPlugins() {
    return query.shouldQueryShowPlugins();
  }

  @Override
  public Component getMotd() {
    if (motdAsComponent == null) {
      motdAsComponent = MiniMessage.miniMessage().deserialize(motd);
    }

    return motdAsComponent;
  }

  @Override
  public List<Component> getMotdHover() {
    if (motdHoverComponents == null) {
      motdHoverComponents = motdHover.stream()
          // Fix for Fast Server Pings < 1.0.8 showing "Anonymous Players" on an empty line.
          // See https://github.com/vansencool/FastServerPings/issues/8
          // May be removed after a while.
          .map(s -> s.isEmpty() ? " " : s)
          .map(MiniMessage.miniMessage()::deserialize)
          .toList();
    }

    return motdHoverComponents;
  }

  @Override
  public int getShowMaxPlayers() {
    return showMaxPlayers;
  }

  @Override
  public boolean isOnlineMode() {
    return onlineMode;
  }

  @Override
  public boolean doesPreventChatReports() {
    return preventsChatReports;
  }

  @Override
  public boolean shouldPreventClientProxyConnections() {
    return preventClientProxyConnections;
  }

  /**
   * Gets the global player info forwarding mode used by the proxy.
   *
   * <p>This setting determines how Velocity forwards player information
   * such as UUIDs, IPs, and profile data to backend servers. It can be one of:
   * <ul>
   *   <li>{@link PlayerInfoForwarding#NONE} - no forwarding</li>
   *   <li>{@link PlayerInfoForwarding#LEGACY} - BungeeCord-style forwarding</li>
   *   <li>{@link PlayerInfoForwarding#BUNGEEGUARD} - BungeeGuard-compatible</li>
   *   <li>{@link PlayerInfoForwarding#MODERN} - Velocity modern forwarding (recommended)</li>
   * </ul>
   *
   * @return the global {@link PlayerInfoForwarding} mode
   */
  public PlayerInfoForwarding getPlayerInfoForwardingMode() {
    return playerInfoForwardingMode;
  }

  /**
   * Gets the secret used for verifying forwarded player info.
   *
   * <p>This secret is required for {@link PlayerInfoForwarding#MODERN} and
   * {@link PlayerInfoForwarding#BUNGEEGUARD} modes. It must be shared securely
   * between the proxy and backend servers.
   *
   * @return a copy of the forwarding secret as a byte array
   */
  public byte[] getForwardingSecret() {
    return forwardingSecret.clone();
  }

  @SuppressWarnings("removal")
  @Override
  public Map<String, String> getServers() {
    Map<String, String> serverAddresses = new HashMap<>();
    getBackendServers().forEach((k, v) -> serverAddresses.put(k, v.address()));
    return serverAddresses;
  }

  @Override
  public Map<String, BackendServerConfig> getBackendServers() {
    return servers.getBackendServers();
  }

  @Override
  public List<String> getAttemptConnectionOrder() {
    return servers.getAttemptConnectionOrder();
  }

  /**
   * Returns the map of custom command aliases configured in the proxy.
   *
   * <p>These aliases are used to redirect alternate command strings to existing proxy commands.
   *
   * @return a map of command names to their associated aliases
   */
  public Map<String, List<String>> getCommandAliases() {
    return commandAliases.getAliases();
  }

  /**
   * Returns the map of proxy command aliases configured in the proxy.
   *
   * <p>These aliases create new commands that execute other commands when invoked.
   * Similar to Bukkit's commands.yml functionality.
   *
   * @return a map of command names to their associated command executions
   */
  public Map<String, List<String>> getProxyCommandAliases() {
    return proxyCommandAliases.getAliases();
  }

  @Override
  public Map<String, List<String>> getForcedHosts() {
    return forcedHosts.getForcedHosts();
  }

  @Override
  public boolean isForcedHostAsFallback() {
    return forcedHosts.isForcedHostAsFallback();
  }

  @Override
  public boolean isCachePlayerProfileResultEnabled() {
    return advanced.isCachePlayerProfileResultEnabled();
  }

  @Override
  public int getProfileCacheExpiryMinutes() {
    return advanced.getProfileCacheExpiryMinutes();
  }

  @Override
  public int getCompressionThreshold() {
    return advanced.getCompressionThreshold();
  }

  @Override
  public int getCompressionLevel() {
    return advanced.getCompressionLevel();
  }

  @Override
  public int getLoginRatelimit() {
    return advanced.getLoginRatelimit();
  }

  @Override
  public Optional<Favicon> getFavicon() {
    return Optional.ofNullable(favicon);
  }

  @Override
  public boolean isAnnounceForge() {
    return announceForge;
  }

  @Override
  public int getConnectTimeout() {
    return advanced.getConnectionTimeout();
  }

  /**
   * Returns whether the <code>/server</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isServerEnabled() {
    return commands.isServerEnabled();
  }

  /**
   * Returns whether the <code>/alert</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isAlertEnabled() {
    return commands.isAlertEnabled();
  }

  /**
   * Returns whether the <code>/alertraw</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isAlertRawEnabled() {
    return commands.isAlertRawEnabled();
  }

  /**
   * Returns whether the <code>/find</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isFindEnabled() {
    return commands.isFindEnabled();
  }

  /**
   * Returns whether the <code>/gkick</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isGkickEnabled() {
    return commands.isGkickEnabled();
  }

  /**
   * Returns whether the <code>/gip</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isGipEnabled() {
    return commands.isGipEnabled();
  }

  /**
   * Returns whether the <code>/transfer</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isTransferEnabled() {
    return commands.isTransferEnabled();
  }

  /**
   * Returns whether the <code>/glist</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isGlistEnabled() {
    return commands.isGlistEnabled();
  }

  /**
   * Returns whether the <code>/plist</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isPlistEnabled() {
    return commands.isPlistEnabled();
  }

  /**
   * Returns whether the <code>/hub</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isHubEnabled() {
    return commands.isHubEnabled();
  }

  /**
   * Returns whether the <code>/ping</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isPingEnabled() {
    return commands.isPingEnabled();
  }

  /**
   * Returns whether the <code>/send</code> command is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isSendEnabled() {
    return commands.isSendEnabled();
  }

  /**
   * Returns whether command usage should override the default <code>/server</code> help.
   *
   * @return {@code true} if overridden
   */
  public boolean isOverrideServerCommandUsage() {
    return commands.isOverrideServerCommandUsage();
  }

  @Override
  public int getReadTimeout() {
    return advanced.getReadTimeout();
  }

  @Override
  public int getCommandRatelimit() {
    return advanced.getCommandRateLimit();
  }

  @Override
  public int getTabCompleteRatelimit() {
    return advanced.getTabCompleteRateLimit();
  }

  @Override
  public int getKickAfterRateLimitedTabCompletes() {
    return advanced.getKickAfterRateLimitedTabCompletes();
  }

  @Override
  public boolean isForwardCommandsIfRateLimited() {
    return advanced.isForwardCommandsIfRateLimited();
  }

  @Override
  public int getKickAfterRateLimitedCommands() {
    return advanced.getKickAfterRateLimitedCommands();
  }

  /**
   * Returns whether the PROXY protocol is enabled for incoming connections.
   *
   * @return {@code true} if PROXY protocol is enabled, {@code false} otherwise
   */
  public boolean isProxyProtocol() {
    return advanced.isProxyProtocol();
  }

  /**
   * Sets whether the PROXY protocol is enabled for incoming connections.
   *
   * @param proxyProtocol {@code true} to enable the protocol, {@code false} to disable it
   */
  public void setProxyProtocol(final boolean proxyProtocol) {
    advanced.setProxyProtocol(proxyProtocol);
  }

  /**
   * Returns whether TCP Fast Open is enabled for the proxy.
   *
   * @return {@code true} if TCP Fast Open is enabled
   */
  public boolean useTcpFastOpen() {
    return advanced.isTcpFastOpen();
  }

  /**
   * Gets the metrics configuration.
   *
   * @return the {@link Metrics} configuration object
   */
  public Metrics getMetrics() {
    return metrics;
  }

  /**
   * Gets the configured ping passthrough mode.
   *
   * @return the {@link PingPassthroughMode} being used
   */
  public PingPassthroughMode getPingPassthrough() {
    return pingPassthrough;
  }

  /**
   * Returns whether to include actual player samples in server ping responses.
   *
   * @return {@code true} if real samples are shown, {@code false} otherwise
   */
  public boolean getSamplePlayersInPing() {
    return samplePlayersInPing;
  }

  /**
   * Returns whether player address logging is enabled.
   *
   * @return {@code true} if IP address logging is enabled
   */
  public boolean isPlayerAddressLoggingEnabled() {
    return enablePlayerAddressLogging;
  }

  /**
   * Returns whether the BungeeCord plugin message channel is enabled.
   *
   * @return {@code true} if enabled
   */
  public boolean isBungeePluginChannelEnabled() {
    return advanced.isBungeePluginMessageChannel();
  }

  /**
   * Returns whether ping requests are logged in the console.
   *
   * @return {@code true} if ping logging is enabled
   */
  public boolean isShowPingRequests() {
    return advanced.isShowPingRequests();
  }

  /**
   * Returns whether the proxy attempts to fail over when a server disconnects unexpectedly.
   *
   * @return {@code true} if failover is enabled
   */
  public boolean isFailoverOnUnexpectedServerDisconnect() {
    return advanced.isFailoverOnUnexpectedServerDisconnect();
  }

  /**
   * Returns whether proxy commands should be announced to players.
   *
   * @return {@code true} if proxy commands are announced
   */
  public boolean isAnnounceProxyCommands() {
    return advanced.isAnnounceProxyCommands();
  }

  /**
   * Returns whether command executions are logged.
   *
   * @return {@code true} if logging is enabled
   */
  public boolean isLogCommandExecutions() {
    return advanced.isLogCommandExecutions();
  }

  /**
   * Returns whether player transfers between proxies are accepted.
   *
   * @return {@code true} if transfers are accepted
   */
  @SuppressWarnings("BooleanMethodIsAlwaysInverted")
  public boolean isAcceptTransfers() {
    return this.advanced.isAcceptTransfers();
  }

  /**
   * Returns whether illegal characters are allowed in chat messages.
   *
   * @return {@code true} if illegal characters are allowed
   */
  public boolean isAllowIllegalCharactersInChat() {
    return advanced.isAllowIllegalCharactersInChat();
  }

  /**
   * Gets the server brand string shown to clients.
   *
   * @return the processed server brand string
   */
  public String getServerBrand() {
    return advanced.getServerBrand();
  }

  /**
   * Gets the fallback version string shown to clients if no backend responds.
   *
   * @return the fallback version string
   */
  public String getFallbackVersionPing() {
    return advanced.getFallbackVersionPing();
  }

  /**
   * Returns whether the proxy always uses the fallback version ping.
   *
   * @return {@code true} if fallback version ping is always used
   */
  public boolean getAlwaysFallBackPing() {
    return advanced.getAlwaysFallBackPing();
  }

  /**
   * Gets the custom proxy brand name shown to players.
   *
   * @return the proxy brand name
   */
  public String getProxyBrandCustom() {
    return advanced.getProxyBrandCustom();
  }

  /**
   * Gets the custom backend brand name shown to players.
   *
   * @return the backend brand name
   */
  public String getBackendBrandCustom() {
    return advanced.getBackendBrandCustom();
  }

  /**
   * Gets the dynamic fallback filter mode configured for server selection.
   *
   * @return the fallback filter identifier
   */
  public DynamicFallbackFilter getDynamicFallbackFilter() {
    return servers.getDynamicFallbackFilter();
  }

  /**
   * Returns whether key authentication is enforced when online mode is enabled.
   *
   * @return {@code true} if key authentication is required
   */
  public boolean isForceKeyAuthentication() {
    return forceKeyAuthentication;
  }

  /**
   * Returns whether the proxy uses SO_REUSEPORT when binding network sockets.
   *
   * @return {@code true} if reuse port is enabled
   */
  public boolean isEnableReusePort() {
    return advanced.isEnableReusePort();
  }

  /**
   * Gets the Redis configuration block.
   *
   * @return the {@link Redis} configuration
   */
  public @NotNull Redis getRedis() {
    return redis;
  }

  /**
   * Gets the Queue configuration block.
   *
   * @return the {@link Queue} configuration
   */
  public @NotNull Queue getQueue() {
    return queue;
  }

  /**
   * Gets the slash command mappings to backend server lists.
   *
   * @return a map of slash command aliases and corresponding server targets
   */
  public Map<String, List<String>> getSlashServers() {
    return slashServers;
  }

  /**
   * Gets the configured server links shown to players.
   *
   * @return a map of link scope names to {@link ServerLink} entries
   */
  public Map<String, List<ServerLink>> getServerLinks() {
    return serverLinks;
  }

  /**
   * Gets the list of proxy addresses configured for load balancing.
   *
   * @return a list of {@link ProxyAddress} entries
   */
  public List<ProxyAddress> getProxyAddresses() {
    return proxyAddresses;
  }

  /**
   * Gets the dynamic proxy filter strategy mode.
   *
   * @return the configured dynamic proxy filter
   */
  public DynamicProxyFilterMode getDynamicProxyFilter() {
    return this.dynamicProxyFilter;
  }

  /**
   * Gets the map of per-server player caps.
   *
   * @return the player caps mapping
   */
  public Map<String, Integer> getPlayerCaps() {
    return this.playerCaps;
  }

  /**
   * Gets all server links scoped to the provided server name, including global ones.
   *
   * @param serverName the backend server name (e.g., "lobby")
   * @return a list of {@link ServerLink} visible to players on that server
   */
  public List<ServerLink> getServerLinksFor(final String serverName) {
    List<ServerLink> result = new ArrayList<>();

    for (Map.Entry<String, List<ServerLink>> entry : this.serverLinks.entrySet()) {
      String scope = entry.getKey();
      if ("all".equalsIgnoreCase(scope) || serverName.equalsIgnoreCase(scope)) {
        result.addAll(entry.getValue());
      }
    }

    return result;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("bind", bind)
        .add("motd", motd)
        .add("motdHover", motdHover)
        .add("showMaxPlayers", showMaxPlayers)
        .add("onlineMode", onlineMode)
        .add("playerInfoForwardingMode", playerInfoForwardingMode)
        .add("forwardingSecret", forwardingSecret)
        .add("announceForge", announceForge)
        .add("servers", servers)
        .add("forcedHosts", forcedHosts)
        .add("commands", commands)
        .add("advanced", advanced)
        .add("query", query)
        .add("redis", redis)
        .add("queue", queue)
        .add("favicon", favicon)
        .add("enablePlayerAddressLogging", enablePlayerAddressLogging)
        .add("forceKeyAuthentication", forceKeyAuthentication)
        .add("logPlayerConnections", logPlayerConnections)
        .add("logPlayerDisconnections", logPlayerDisconnections)
        .add("logOfflineConnections", logOfflineConnections)
        .add("disableForge", disableForge)
        .add("enforceChatSigning", enforceChatSigning)
        .add("preventsChatReports", preventsChatReports)
        .add("translateHeaderFooter", translateHeaderFooter)
        .add("logMinimumVersion", logMinimumVersion)
        .add("minimumVersion", minimumVersion)
        .add("slashServers", slashServers)
        .add("playerCaps", playerCaps)
        .toString();
  }

  /**
   * Reads the Velocity configuration from {@code path}.
   *
   * @param path the path to read from
   * @return the deserialized Velocity configuration
   * @throws IOException if we could not read from the {@code path}.
   */
  public static VelocityConfiguration read(final Path path) throws IOException {
    URL defaultConfigLocation = VelocityConfiguration.class.getClassLoader()
        .getResource("default-velocity.toml");
    if (defaultConfigLocation == null) {
      throw new RuntimeException("Default configuration file does not exist.");
    }

    // Explicitly create the default configuration file if it does not exist. This
    // ensures a complete file is present before it is written to the disk.
    if (Files.notExists(path)) {
      try (InputStream in = defaultConfigLocation.openStream()) {
        Files.copy(in, path);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create default configuration file at " + path + ".", e);
      }
    }

    // Create the forwarding-secret file on first-time startup if it doesn't exist.
    final Path defaultForwardingSecretPath = Path.of("forwarding.secret");
    if (Files.notExists(path) && Files.notExists(defaultForwardingSecretPath)) {
      Files.writeString(defaultForwardingSecretPath, generateRandomString(12));
    }

    try (CommentedFileConfig config = CommentedFileConfig.builder(path)
            .defaultData(defaultConfigLocation)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build()) {
      config.load();

      try {
        ConfigDetector detector = new ConfigDetector(LOGGER);
        ConfigDetector.ConfigAnalysis analysis = detector.analyzeConfiguration(path);

        if (!analysis.missingOptions().isEmpty()) {
          LOGGER.warn("Missing configuration options: {}", String.join(", ", analysis.missingOptions()));
          LOGGER.warn("Run /velocity configcheck for full details");
        }
      } catch (IOException e) {
        LOGGER.debug("Could not perform configuration check during configuration loading", e);
      }

      final List<ConfigurationMigration> migrations = new ArrayList<>(List.of(
          new ForwardingMigration(),
          new KeyAuthenticationMigration(),
          new MotdMigration(),
          new MiniMessageTranslationsMigration(),
          new TransferIntegrationMigration()
      ));

      migrations.addAll(CtdConfigMigrations.createCtdMigrations());

      for (final ConfigurationMigration migration : migrations) {
        if (migration.shouldMigrate(config)) {
          migration.migrate(config, LOGGER);
        }
      }

      String forwardingSecretString = System.getenv().getOrDefault(
          "VELOCITY_FORWARDING_SECRET", "");
      if (forwardingSecretString.isBlank()) {
        final String forwardSecretFile = config.get("forwarding-secret-file");
        final Path secretPath = forwardSecretFile == null
            ? defaultForwardingSecretPath
            : Path.of(forwardSecretFile);
        if (Files.exists(secretPath)) {
          if (Files.isRegularFile(secretPath)) {
            forwardingSecretString = String.join("", Files.readAllLines(secretPath));
          } else {
            throw new RuntimeException(
                "The file " + forwardSecretFile + " is not a valid file or it is a directory.");
          }
        } else {
          Files.createFile(secretPath);
          Files.writeString(secretPath, forwardingSecretString = generateRandomString(12), StandardCharsets.UTF_8);
          LOGGER.info("The forwarding-secret-file does not exist. A new file has been created at {}", forwardSecretFile);
        }
      }

      final byte[] forwardingSecret = forwardingSecretString.getBytes(StandardCharsets.UTF_8);
      final String motd = config.getOrElse("motd", "<#09add3>A Velocity Server");
      final List<String> motdHover = config.getOrElse("motd-hover", new ArrayList<>());

      // Read the rest of the config
      final CommentedConfig serversConfig = config.get("servers");
      final CommentedConfig forcedHostsConfig = config.get("forced-hosts");
      final CommentedConfig commandAliasesConfig = config.get("command-aliases");
      final CommentedConfig proxyCommandAliasesConfig = config.get("proxy-command-aliases");
      final CommentedConfig commandsConfig = config.get("commands");
      final CommentedConfig advancedConfig = config.get("advanced");
      final CommentedConfig queryConfig = config.get("query");
      final CommentedConfig metricsConfig = config.get("metrics");
      final CommentedConfig redisConfig = config.get("redis");
      final CommentedConfig queueConfig = config.get("queue");
      final CommentedConfig serverLinksConfig = config.get("server-links");
      final CommentedConfig proxyAddressesConfig = config.get("proxy-addresses");
      final CommentedConfig playerCapsConfig = config.get("playercaps");
      final PlayerInfoForwarding forwardingMode = config.getEnumOrElse(
              "player-info-forwarding-mode", PlayerInfoForwarding.NONE);
      final PingPassthroughMode pingPassthroughMode = config.getEnumOrElse("ping-passthrough",
              PingPassthroughMode.DISABLED);
      final boolean samplePlayersInPing = config.getOrElse("sample-players-in-ping", false);
      final String bind = config.getOrElse("bind", "0.0.0.0:25565");
      final int maxPlayers = config.getIntOrElse("show-max-players", 500);
      final boolean onlineMode = config.getOrElse("online-mode", true);
      final boolean forceKeyAuthentication = config.getOrElse("force-key-authentication", true);
      final boolean announceForge = config.getOrElse("announce-forge", true);
      final boolean preventClientProxyConnections = config.getOrElse(
              "prevent-client-proxy-connections", false);
      final boolean kickExisting = config.getOrElse("kick-existing-players", false);
      final boolean kickExistingCheckIp = config.getOrElse("kick-existing-players-check-ip", false);
      final boolean enablePlayerAddressLogging = config.getOrElse(
              "enable-player-address-logging", true);
      final boolean logPlayerConnections = config.getOrElse(
              "log-player-connections", true);
      final boolean logPlayerDisconnections = config.getOrElse(
              "log-player-disconnections", true);
      final boolean logOfflineConnections = config.getOrElse(
              "log-offline-connections", true);
      final boolean disableForge = config.getOrElse("disable-forge", false);
      final boolean enforceChatSigning = config.getOrElse(
              "enforce-chat-signing", false);
      final boolean preventsChatReports = config.getOrElse(
              "prevents-chat-reports", false);
      final boolean translateHeaderFooter = config.getOrElse(
              "translate-header-footer", true);
      final boolean logMinimumVersion = config.getOrElse(
              "log-minimum-version", false);
      final String minimumVersion = config.getOrElse("minimum-version", "1.7.2");
      final String maximumVersion = config.getOrElse("maximum-version", UNBOUNDED);
      final CommentedConfig slashServersConfig = config.getOrElse("slash-servers", (CommentedConfig) null);
      final Map<String, List<String>> slashServers = new HashMap<>();
      if (slashServersConfig != null) {
        for (UnmodifiableConfig.Entry entry : slashServersConfig.entrySet()) {
          if (entry.getValue() instanceof String) {
            slashServers.put(entry.getKey(), ImmutableList.of(entry.getValue()));
          } else if (entry.getValue() instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> value = ImmutableList.copyOf((List<String>) entry.getValue());
            slashServers.put(entry.getKey(), value);
          } else {
            LOGGER.warn("Invalid value of type {} in slash servers!", entry.getValue().getClass());
          }
        }
      }

      final Map<String, List<ServerLink>> links = new HashMap<>();
      if (serverLinksConfig != null) {
        for (CommentedConfig.Entry entry : serverLinksConfig.entrySet()) {
          CommentedConfig link = entry.getValue();
          String label = link.get("label");
          String url = link.get("link");
          Object serverName = link.get("server");
          if (!(serverName instanceof List<?> serverList)) {
            LOGGER.warn("Invalid 'server' value for server-link '{}'. Expected a list of servers like \"factions\" or \"minigames\"", entry.getKey());
            continue;
          }

          List<String> scopes = serverList.stream()
              .filter(Objects::nonNull)
              .map(Object::toString)
              .map(String::trim)
              .toList();

          if (label != null && url != null) {
            for (String scope : scopes) {
              links.computeIfAbsent(scope, s -> new ArrayList<>())
                  .add(ServerLink.serverLink(MiniMessage.miniMessage().deserialize(label), url));
            }
          }
        }
      }

      DynamicProxyFilterMode filter = DynamicProxyFilterMode.MOST_EMPTY;
      List<ProxyAddress> addresses = new ArrayList<>();
      if (proxyAddressesConfig != null) {
        filter = proxyAddressesConfig.getEnumOrElse("dynamic-proxy-filter", filter);

        for (CommentedConfig.Entry entry : proxyAddressesConfig.entrySet()) {
          if (entry.getKey().equalsIgnoreCase("dynamic-proxy-filter")) {
            continue;
          }

          CommentedConfig link = entry.getValue();
          addresses.add(new ProxyAddress(link.get("proxy-id"),
              link.get("ip"),
              link.get("port")));
        }
      }

      final Map<String, Integer> playerCaps = new HashMap<>();
      if (playerCapsConfig != null) {
        for (CommentedConfig.Entry entry : playerCapsConfig.entrySet()) {
          playerCaps.put(entry.getKey(), entry.getValue());
        }
      }

      // Throw an exception if the forwarding-secret file is empty and the proxy is using a
      // forwarding mode that requires it.
      if (forwardingSecret.length == 0
              && (forwardingMode == PlayerInfoForwarding.MODERN
              || forwardingMode == PlayerInfoForwarding.BUNGEEGUARD)) {
        throw new RuntimeException("The forwarding-secret file must not be empty.");
      }

      return new VelocityConfiguration(
          bind,
          motd,
          motdHover,
          maxPlayers,
          onlineMode,
          preventClientProxyConnections,
          announceForge,
          forwardingMode,
          forwardingSecret,
          kickExisting,
          kickExistingCheckIp,
          pingPassthroughMode,
          samplePlayersInPing,
          enablePlayerAddressLogging,
          new Servers(serversConfig),
          new ForcedHosts(forcedHostsConfig),
          new CommandAliases(commandAliasesConfig),
          new ProxyCommandAliases(proxyCommandAliasesConfig),
          new Commands(commandsConfig),
          new Advanced(advancedConfig),
          new Query(queryConfig),
          new Metrics(metricsConfig),
          forceKeyAuthentication,
          logPlayerConnections,
          logPlayerDisconnections,
          logOfflineConnections,
          disableForge,
          enforceChatSigning,
          preventsChatReports,
          translateHeaderFooter,
          logMinimumVersion,
          minimumVersion,
          maximumVersion,
          new Redis(redisConfig),
          new Queue(queueConfig),
          slashServers,
          links,
          addresses,
          filter,
          playerCaps
      );
    }
  }

  /**
   * Generates a Random String.
   *
   * @param length the required string size.
   * @return a new random string.
   */
  public static String generateRandomString(final int length) {
    final String chars = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
    final StringBuilder builder = new StringBuilder();
    final Random rnd = new SecureRandom();
    for (int i = 0; i < length; i++) {
      builder.append(chars.charAt(rnd.nextInt(chars.length())));
    }

    return builder.toString();
  }

  /**
   * Determines whether Velocity should kick any existing player session that shares a UUID with
   * an incoming connection, allowing the new connection to take over.
   *
   * <p>Works in both online and offline mode. In offline mode the UUID is derived from the
   * username, so this setting is unsafe unless {@link #isKickExistingPlayersCheckIp()} is also
   * enabled to restrict kicks to same-IP reconnects.
   *
   * @return true if existing players with matching UUIDs should be kicked
   */
  public boolean isKickExistingPlayers() {
    return kickExistingPlayers;
  }

  /**
   * Determines whether kick-existing-players should only fire when the new connection comes from
   * the same IP address as the existing session.
   *
   * <p>When {@code true}: a duplicate UUID from the same IP kicks the existing session. A duplicate
   * UUID from a different IP is denied instead, leaving the existing player unaffected.
   *
   * <p>When {@code false}: any duplicate UUID kicks the existing session unconditionally.
   *
   * @return true if the kick is restricted to same-IP connections
   */
  public boolean isKickExistingPlayersCheckIp() {
    return kickExistingPlayersCheckIp;
  }

  /**
   * Returns whether Velocity should log all player connection attempts.
   *
   * @return true if player connections should be logged
   */
  public boolean isLogPlayerConnections() {
    return logPlayerConnections;
  }

  /**
   * Returns whether Velocity should log when players disconnect from the proxy.
   *
   * @return true if player disconnections should be logged
   */
  public boolean isLogPlayerDisconnections() {
    return logPlayerDisconnections;
  }

  /**
   * Returns whether Velocity should log connections that fail to authenticate
   * or timeout before login completes.
   *
   * @return true if unauthenticated/failed connections should be logged
   */
  public boolean isLogOfflineConnections() {
    return logOfflineConnections;
  }

  /**
   * Returns whether support for Forge clients and plugin messages is disabled.
   *
   * <p>Disabling this helps reduce compatibility issues or plugin message spam
   * for proxies not supporting Forge negotiation.
   *
   * @return true if Forge support is disabled
   */
  public boolean isDisableForge() {
    return disableForge;
  }

  /**
   * Returns whether clients are required to use Mojang's chat signing
   * mechanism introduced in 1.19+.
   *
   * <p>Enabling this improves security and message authenticity, but may
   * impact compatibility with unsigned chat modifications.
   *
   * @return true if chat signing enforcement is enabled
   */
  public boolean enforceChatSigning() {
    return enforceChatSigning;
  }

  /**
   * Returns whether Velocity should convert MiniMessage-formatted headers/footers
   * into legacy text format for older clients.
   *
   * @return true if header/footer translation is enabled
   */
  public boolean isTranslateHeaderFooter() {
    return translateHeaderFooter;
  }

  /**
   * Returns whether Velocity should log the minimum supported client version
   * to the console during startup.
   *
   * @return true if the minimum version should be logged
   */
  public boolean isLogMinimumVersion() {
    return logMinimumVersion;
  }

  /**
   * Gets the minimum allowed Minecraft version that can connect to the proxy.
   *
   * <p>Clients connecting with a lower version than this will be rejected.
   *
   * @return the minimum supported version string (e.g., {@code "1.7.2"})
   */
  public String getMinimumVersion() {
    return minimumVersion;
  }

  /**
   * Gets the minimum allowed Minecraft version for a specific server.
   *
   * <p>If the server has a specific minimum version configured, that value is returned.
   * Otherwise, the global minimum version is returned.
   *
   * @param serverName the name of the server to check
   * @return the minimum supported version string for the server (e.g., {@code "1.7.2"})
   */
  public String getMinimumVersionForServer(final String serverName) {
    return servers.getServerMinimumVersions().getOrDefault(serverName, minimumVersion);
  }

  /**
   * Gets the maximum allowed Minecraft version that can connect to the proxy.
   *
   * <p>Clients connecting with a higher version than this will be rejected.
   * Returns an empty optional if no maximum version limit is configured (UNBOUNDED).
   *
   * @return the maximum supported version string (e.g., {@code "1.21.10"}), or empty if unbounded
   */
  public Optional<String> getMaximumVersion() {
    return isUnbounded(maximumVersion) ? Optional.empty() : Optional.of(maximumVersion);
  }

  /**
   * Gets the maximum allowed Minecraft version for a specific server.
   *
   * <p>If the server has a specific maximum version configured, that value is returned.
   * Otherwise, the global maximum version is used.
   * Returns an empty optional if the effective maximum version is UNBOUNDED.
   *
   * @param serverName the name of the server to check
   * @return the maximum supported version string for the server, or empty if unbounded
   */
  public Optional<String> getMaximumVersionForServer(final String serverName) {
    final String effective = servers.getServerMaximumVersions().getOrDefault(serverName, maximumVersion);
    return isUnbounded(effective) ? Optional.empty() : Optional.of(effective);
  }

  private static boolean isUnbounded(final String version) {
    return UNBOUNDED.equalsIgnoreCase(version);
  }

  /**
   * Gets a list of aliases that invoke the {@code /server} command or its variations.
   *
   * <p>These aliases are registered for convenience (e.g. {@code /queue}, {@code /joinqueue}).
   *
   * @return a list of server command aliases
   */
  public List<String> getServerAliases() {
    return this.servers.getServerAliases();
  }

  private static final class Servers {

    /**
     * The configured mapping of backend servers available for player connections.
     *
     * <p>The key is the unique server name used within the proxy (for example,
     * {@code "lobby"}), and the value is a {@link BackendServerConfig} describing
     * the backend server's address and its {@link ServerInfoForwardingMode}.</p>
     *
     * <p>This map determines the set of servers players can connect to and
     * specifies whether each server should inherit the global forwarding mode
     * or use its own explicit mode.</p>
     */
    private Map<String, BackendServerConfig> servers = ImmutableMap.of(
        "lobby", new BackendServerConfig("127.0.0.1:30066"),
        "factions", new BackendServerConfig("127.0.0.1:30067", ServerInfoForwardingMode.MODERN),
        "minigames", new BackendServerConfig("127.0.0.1:30068", ServerInfoForwardingMode.LEGACY)
    );

    /**
     * The ordered list of fallback servers to try when connecting a player.
     *
     * <p>This list defines the default connection priority. If a connection to the
     * first server fails, the proxy tries the next one in order, and so on.
     */
    private List<String> attemptConnectionOrder = ImmutableList.of("lobby");

    /**
     * Per-server overrides for minimum version requirements.
     *
     * <p>If a server is listed here, it uses the specified minimum version
     * instead of the global configuration.
     */
    private Map<String, String> serverMinimumVersions = ImmutableMap.of();

    /**
     * Per-server overrides for maximum version requirements.
     *
     * <p>If a server is listed here, it uses the specified maximum version
     * instead of the global configuration.
     */
    private Map<String, String> serverMaximumVersions = ImmutableMap.of();

    /**
     * The strategy used for choosing a fallback server when {@code attemptConnectionOrder}
     * fails or is bypassed (e.g., in multi-proxy setups).
     *
     * <p>Common values include {@code "FIRST_AVAILABLE"}, {@code "MOST_POPULATED"},
     * or {@code "LEAST_POPULATED"}.
     */
    private DynamicFallbackFilter dynamicFallbackFilter;

    /**
     * A list of aliases that invoke server-related commands.
     *
     * <p>These aliases allow players to use commands like {@code /queue} or {@code /server}
     * interchangeably to access the same routing logic.
     */
    @Expose
    private List<String> serverAliases;

    private Servers() {
    }

    private Servers(final CommentedConfig config) {
      this.serverAliases = List.of("joinqueue", "queue", "server");
      this.dynamicFallbackFilter = DynamicFallbackFilter.FIRST_AVAILABLE;

      if (config != null) {
        Map<String, BackendServerConfig> servers = new HashMap<>();
        Map<String, String> serverMinimumVersions = new HashMap<>();
        Map<String, String> serverMaximumVersions = new HashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          if (entry.getKey().equalsIgnoreCase("dynamic-fallbacks-filter")) {
            continue;
          }

          if (entry.getValue() instanceof CommentedConfig c) {
            String address = null;
            ServerInfoForwardingMode forwardingMode = null;
            for (UnmodifiableConfig.Entry entry2 : c.entrySet()) {
              if (entry2.getKey().equalsIgnoreCase("address")) {
                address = entry2.getValue();
              }

              if (entry2.getKey().equalsIgnoreCase("forwarding-mode")) {
                String forwardingModeName = entry2.getValue();
                forwardingMode = ServerInfoForwardingMode.valueOf(
                    forwardingModeName.toUpperCase(Locale.ROOT));
              }

              if (entry2.getKey().equalsIgnoreCase("minimum-version")) {
                serverMinimumVersions.put(cleanValue(entry.getKey()), entry2.getValue());
              }

              if (entry2.getKey().equalsIgnoreCase("maximum-version")) {
                serverMaximumVersions.put(cleanValue(entry.getKey()), entry2.getValue());
              }
            }

            if (address == null) {
              throw new IllegalArgumentException("Server entry " + entry.getKey() + " is missing address!");
            }

            servers.put(cleanValue(entry.getKey()), new BackendServerConfig(address, forwardingMode));
            // Support for old server config system (forwarding mode will be null)
          } else if (entry.getValue() instanceof String v) {
            servers.put(cleanValue(entry.getKey()), new BackendServerConfig(v));
          } else {
            if (!entry.getKey().equalsIgnoreCase("try")
                && !entry.getKey().equalsIgnoreCase("dynamic-fallbacks-filter")
                && !entry.getKey().equalsIgnoreCase("server-aliases")) {
              throw new IllegalArgumentException(
                  "Server entry " + entry.getKey() + " is not a server!");
            }
          }
        }

        this.servers = ImmutableMap.copyOf(servers);
        this.serverMinimumVersions = ImmutableMap.copyOf(serverMinimumVersions);
        this.serverMaximumVersions = ImmutableMap.copyOf(serverMaximumVersions);
        this.attemptConnectionOrder = config.getOrElse("try", attemptConnectionOrder).stream().toList();
        this.dynamicFallbackFilter = config.getEnumOrElse("dynamic-fallbacks-filter", DynamicFallbackFilter.FIRST_AVAILABLE);
        this.serverAliases = config.getOrElse("server-aliases", List.of("joinqueue", "queue", "server"));
      }
    }

    public List<String> getServerAliases() {
      return serverAliases != null ? serverAliases : List.of("joinqueue", "queue", "server");
    }

    private Map<String, BackendServerConfig> getBackendServers() {
      return servers;
    }

    public void setServers(final Map<String, BackendServerConfig> servers) {
      this.servers = servers;
    }

    public List<String> getAttemptConnectionOrder() {
      return attemptConnectionOrder;
    }

    public DynamicFallbackFilter getDynamicFallbackFilter() {
      return dynamicFallbackFilter;
    }

    public void setAttemptConnectionOrder(final List<String> attemptConnectionOrder) {
      this.attemptConnectionOrder = attemptConnectionOrder;
    }

    public Map<String, String> getServerMinimumVersions() {
      return serverMinimumVersions;
    }

    public void setServerMinimumVersions(final Map<String, String> serverMinimumVersions) {
      this.serverMinimumVersions = serverMinimumVersions;
    }

    public Map<String, String> getServerMaximumVersions() {
      return serverMaximumVersions;
    }

    public void setServerMaximumVersions(final Map<String, String> serverMaximumVersions) {
      this.serverMaximumVersions = serverMaximumVersions;
    }

    /**
     * TOML requires keys to match a regex of {@code [A-Za-z0-9_-]} unless it is wrapped in quotes;
     * however, the TOML parser returns the key with the quotes so we need to clean the value.
     *
     * @param value the value to clean
     * @return the cleaned value
     */
    private String cleanValue(final String value) {
      return value.replace("\"", "");
    }

    @Override
    public String toString() {
      return "Servers{"
          + "servers=" + servers
          + ", attemptConnectionOrder=" + attemptConnectionOrder
          + ", serverMinimumVersions=" + serverMinimumVersions
          + ", serverMaximumVersions=" + serverMaximumVersions
          + '}';
    }
  }

  private static final class CommandAliases {

    /**
     * A map of command aliases defined in the configuration.
     *
     * <p>Each key is a command alias (e.g., "hub", "glist"), and the value is a list of
     * actual commands or targets that the alias resolves to (e.g., ["server lobby"]).</p>
     *
     * <p>This allows multiple alternate command inputs to be mapped to the same base command.</p>
     */
    private final Map<String, List<String>> aliases;

    private CommandAliases(final CommentedConfig config) {
      Map<String, List<String>> parsed = new HashMap<>();
      if (config != null) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          Object value = entry.getValue();
          if (value instanceof List<?> list) {
            parsed.put(entry.getKey(), list.stream().map(Object::toString).toList());
          } else if (value instanceof String str) {
            parsed.put(entry.getKey(), List.of(str));
          } else {
            LOGGER.warn("Invalid value in [command-aliases] for '{}': {}", entry.getKey(), value);
          }
        }
      }

      this.aliases = ImmutableMap.copyOf(parsed);
    }

    public Map<String, List<String>> getAliases() {
      return aliases;
    }

    @Override
    public String toString() {
      return "CommandAliases{"
          + "aliases=" + aliases
          + '}';
    }
  }

  private static final class ProxyCommandAliases {

    /**
     * A map of proxy command aliases defined in the configuration.
     *
     * <p>Each key is a new command name (e.g., "help"), and the value is a list of
     * commands to execute when this command is invoked (e.g., ["velocity info"]).</p>
     *
     * <p>This allows creating new commands that execute other commands, similar to Bukkit's commands.yml.</p>
     */
    private final Map<String, List<String>> aliases;

    private ProxyCommandAliases(final CommentedConfig config) {
      Map<String, List<String>> parsed = new HashMap<>();
      if (config != null) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          Object value = entry.getValue();
          if (value instanceof List<?> list) {
            parsed.put(entry.getKey(), list.stream().map(Object::toString).toList());
          } else if (value instanceof String str) {
            parsed.put(entry.getKey(), List.of(str));
          } else {
            LOGGER.warn("Invalid value in [proxy-command-aliases] for '{}': {}", entry.getKey(), value);
          }
        }
      }

      this.aliases = ImmutableMap.copyOf(parsed);
    }

    public Map<String, List<String>> getAliases() {
      return aliases;
    }

    @Override
    public String toString() {
      return "ProxyCommandAliases{"
          + "aliases=" + aliases
          + '}';
    }
  }

  private static final class ForcedHosts {

    /**
     * A mapping of virtual hostnames to server connection targets.
     *
     * <p>Each key represents a hostname (e.g., {@code play.example.com}) and its
     * corresponding value is a list of backend server names that should be attempted
     * in order when a player connects using that hostname.</p>
     *
     * <p>Used to route players based on the hostname they use to connect.</p>
     */
    private Map<String, List<String>> forcedHosts = ImmutableMap.of();

    private boolean forcedHostAsFallback = true;

    private ForcedHosts() {
    }

    private ForcedHosts(final CommentedConfig config) {
      if (config != null) {
        forcedHostAsFallback = config.get("forced-host-as-fallback");

        Map<String, List<String>> forcedHosts = new HashMap<>();
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
          String key = entry.getKey().toLowerCase(Locale.ROOT);
          if (key.equals("forced-host-as-fallback")) {
            continue;
          }

          if (entry.getValue() instanceof String) {
            forcedHosts.put(key, ImmutableList.of(entry.getValue()));
          } else if (entry.getValue() instanceof List) {
            forcedHosts.put(key, ImmutableList.copyOf((List<String>) entry.getValue()));
          } else {
            LOGGER.warn("Invalid value of type {} in forced hosts!", entry.getValue().getClass());
          }
        }

        this.forcedHosts = ImmutableMap.copyOf(forcedHosts);
      }
    }

    private ForcedHosts(final Map<String, List<String>> forcedHosts) {
      this.forcedHosts = forcedHosts;
    }

    private Map<String, List<String>> getForcedHosts() {
      return forcedHosts;
    }

    private void setForcedHosts(final Map<String, List<String>> forcedHosts) {
      this.forcedHosts = forcedHosts;
    }

    private boolean isForcedHostAsFallback() {
      return forcedHostAsFallback;
    }

    private void setForcedHostAsFallback(boolean forcedHostAsFallback) {
      this.forcedHostAsFallback = forcedHostAsFallback;
    }

    @Override
    public String toString() {
      return "ForcedHosts{"
          + "forcedHosts=" + forcedHosts
          + '}';
    }
  }

  private static final class Commands {

    /**
     * Whether the /server command is enabled.
     * Allows players to view and connect to available servers.
     */
    @Expose
    private boolean serverCommand = true;

    /**
     * Whether the /alert command is enabled.
     * Allows sending broadcast messages to all players across the proxy.
     */
    @Expose
    private boolean alertCommand = true;

    /**
     * Whether the /alertraw command is enabled.
     * Sends raw JSON-formatted broadcast messages to all players.
     */
    @Expose
    private boolean alertRawCommand = true;

    /**
     * Whether the /find command is enabled.
     * Lets users locate which server a specific player is on.
     */
    @Expose
    private boolean findCommand = true;

    /**
     * Whether the /gkick command is enabled.
     * Allows operators to kick players across the entire network.
     */
    @Expose
    private boolean gkickCommand = true;

    /**
     * Whether the /gip command is enabled.
     * Allows operators to retrieve the IP address of an online player.
     */
    @Expose
    private boolean gipCommand = true;

    /**
     * Whether the /glist command is enabled.
     * Displays a list of all online players across all servers.
     */
    @Expose
    private boolean glistCommand = true;

    /**
     * Whether the /plist command is enabled.
     * Displays a list of players on the current server.
     */
    @Expose
    private boolean plistCommand = true;

    /**
     * Whether the /hub command is enabled.
     * Sends players to the default fallback server, typically the hub/lobby.
     */
    @Expose
    private boolean hubCommand = true;

    /**
     * Whether the /ping command is enabled.
     * Allows players to measure their latency to the proxy.
     */
    @Expose
    private boolean pingCommand = true;

    /**
     * Whether the /send command is enabled.
     * Allows transferring players between backend servers.
     */
    @Expose
    private boolean sendCommand = true;

    /**
     * Whether the usage message for /server should override backend-provided messages.
     */
    @Expose
    private boolean overrideServerCommandUsage = false;

    /**
     * Whether the /transfer command is enabled.
     * Allows players to transfer between proxies in a multi-proxy setup.
     */
    @Expose
    private boolean transferEnabled = true;

    private Commands() {
    }

    private Commands(final CommentedConfig config) {
      if (config != null) {
        this.serverCommand = config.getOrElse("server-enabled", true);
        this.alertCommand = config.getOrElse("alert-enabled", true);
        this.alertRawCommand = config.getOrElse("alertraw-enabled", true);
        this.findCommand = config.getOrElse("find-enabled", true);
        this.gkickCommand = config.getOrElse("gkick-enabled", true);
        this.gipCommand = config.getOrElse("gip-enabled", true);
        this.glistCommand = config.getOrElse("glist-enabled", true);
        this.plistCommand = config.getOrElse("plist-enabled", true);
        this.hubCommand = config.getOrElse("hub-enabled", true);
        this.pingCommand = config.getOrElse("ping-enabled", true);
        this.sendCommand = config.getOrElse("send-enabled", true);
        this.overrideServerCommandUsage = config.getOrElse("override-server-command-usage", false);
        this.transferEnabled = config.getOrElse("transfer-enabled", true);
      }
    }

    public boolean isServerEnabled() {
      return serverCommand;
    }

    public boolean isAlertEnabled() {
      return alertCommand;
    }

    public boolean isAlertRawEnabled() {
      return alertRawCommand;
    }

    public boolean isFindEnabled() {
      return findCommand;
    }

    public boolean isGkickEnabled() {
      return gkickCommand;
    }

    public boolean isGipEnabled() {
      return gipCommand;
    }

    public boolean isGlistEnabled() {
      return glistCommand;
    }

    public boolean isPlistEnabled() {
      return plistCommand;
    }

    public boolean isHubEnabled() {
      return hubCommand;
    }

    public boolean isPingEnabled() {
      return pingCommand;
    }

    public boolean isSendEnabled() {
      return sendCommand;
    }

    public boolean isOverrideServerCommandUsage() {
      return overrideServerCommandUsage;
    }

    public boolean isTransferEnabled() {
      return transferEnabled;
    }

    @Override
    public String toString() {
      return "Commands{"
          + "serverCommand=" + serverCommand
          + ", alertCommand=" + alertCommand
          + ", alertRawCommand=" + alertRawCommand
          + ", findCommand=" + findCommand
          + ", glistCommand=" + glistCommand
          + ", plistCommand=" + plistCommand
          + ", hubCommand=" + hubCommand
          + ", pingCommand=" + pingCommand
          + ", sendCommand=" + sendCommand
          + ", overrideServerCommandUsage=" + overrideServerCommandUsage
          + '}';
    }
  }

  private static final class Advanced {

    /**
     * Whether to cache player profile results retrieved from Mojang session servers.
     */
    @Expose
    private boolean cachePlayerProfileResult = false;

    /**
     * How long (in minutes) cached player profiles are retained before expiring.
     */
    @Expose
    private int profileCacheExpiryMinutes = 1440;

    /**
     * The size threshold (in bytes) at which packets are compressed.
     * -1 disables compression, 0 compresses all packets.
     */
    @Expose
    private int compressionThreshold = 256;

    /**
     * The compression level (0–9) used for packet compression.
     * -1 uses the system default.
     */
    @Expose
    private int compressionLevel = -1;

    /**
     * The time (in milliseconds) that must pass before a player can log in again.
     */
    @Expose
    private int loginRatelimit = 3000;

    /**
     * The timeout (in milliseconds) for establishing a connection to a backend server.
     */
    @Expose
    private int connectionTimeout = 5000;

    /**
     * The timeout (in milliseconds) for reading packets from a backend server.
     */
    @Expose
    private int readTimeout = 30000;

    /**
     * Whether to enable support for the HAProxy PROXY protocol.
     */
    @Expose
    private boolean proxyProtocol = false;

    /**
     * Whether to enable TCP Fast Open (may require OS-level support).
     */
    @Expose
    private boolean tcpFastOpen = false;

    /**
     * Whether to enable support for the BungeeCord plugin messaging channel.
     */
    @Expose
    private boolean bungeePluginMessageChannel = true;

    /**
     * Whether to log incoming ping requests in the console.
     */
    @Expose
    private boolean showPingRequests = false;

    /**
     * Whether to failover players to other servers if they are unexpectedly disconnected.
     */
    @Expose
    private boolean failoverOnUnexpectedServerDisconnect = true;

    /**
     * Whether to include proxy command aliases (e.g. `/server`) in tab-completions.
     */
    @Expose
    private boolean announceProxyCommands = true;

    /**
     * Whether to log every command executed by players.
     */
    @Expose
    private boolean logCommandExecutions = false;

    /**
     * Whether to accept `/transfer` messages from other proxies in a multi-proxy network.
     */
    @Expose
    private boolean acceptTransfers = false;

    /**
     * Whether to enable SO_REUSEPORT if supported by the OS.
     */
    @Expose
    private boolean enableReusePort = false;

    /**
     * Maximum number of commands per second before a player is rate-limited.
     */
    @Expose
    private int commandRateLimit = 50;

    /**
     * Whether to allow rate-limited commands to be forwarded anyway (best-effort).
     */
    @Expose
    private boolean forwardCommandsIfRateLimited = true;

    /**
     * Number of rate-limited commands before a player is kicked.
     * 0 disables kicking.
     */
    @Expose
    private int kickAfterRateLimitedCommands = 0;

    /**
     * Maximum number of tab-complete requests per second before rate-limiting is applied.
     */
    @Expose
    private int tabCompleteRateLimit = 10;

    /**
     * Number of rate-limited tab-complete requests before a player is kicked.
     * 0 disables kicking.
     */
    @Expose
    private int kickAfterRateLimitedTabCompletes = 0;

    /**
     * Whether to allow illegal characters in player chat messages.
     * May improve compatibility with older or modified clients.
     */
    @Expose
    private boolean allowIllegalCharactersInChat = false;

    /**
     * The display string for the backend brand, typically shown in debug tools.
     */
    @Expose
    private String serverBrand = "{backend-brand} ({proxy-brand})";

    /**
     * Legacy-formatted string of {@link #serverBrand}, generated at runtime.
     */
    @Expose
    private String serverBrandAsString;

    /**
     * The version string shown in the ping response when a backend is unavailable.
     */
    @Expose
    private String fallbackVersionPing = "{proxy-brand} {protocol-min}-{protocol-max}";

    /**
     * Legacy-formatted version of {@link #fallbackVersionPing}, generated at runtime.
     */
    @Expose
    private String fallbackVersionPingAsString;

    /**
     * Whether to always display the fallback version in ping, even if backends respond normally.
     */
    @Expose
    private boolean alwaysFallBackPing = true;

    /**
     * Custom brand name shown for the proxy in debug/version displays.
     */
    @Expose
    private String proxyBrandCustom = "Velocity";

    /**
     * Custom brand name shown for the backend server in debug/version displays.
     */
    @Expose
    private String backendBrandCustom = "Paper";

    private Advanced() {
    }

    private Advanced(final CommentedConfig config) {
      if (config != null) {
        this.cachePlayerProfileResult = config.getOrElse("cache-player-profile-result", false);
        this.profileCacheExpiryMinutes = config.getOrElse("cache-profile-expiry-minutes", 1440);
        this.compressionThreshold = config.getIntOrElse("compression-threshold", 256);
        this.compressionLevel = config.getIntOrElse("compression-level", -1);
        this.loginRatelimit = config.getIntOrElse("login-ratelimit", 3000);
        this.connectionTimeout = config.getIntOrElse("connection-timeout", 5000);
        this.readTimeout = config.getIntOrElse("read-timeout", 30000);
        if (config.contains("haproxy-protocol")) {
          this.proxyProtocol = config.getOrElse("haproxy-protocol", false);
        } else {
          this.proxyProtocol = config.getOrElse("proxy-protocol", false);
        }
        this.tcpFastOpen = config.getOrElse("tcp-fast-open", false);
        this.bungeePluginMessageChannel = config.getOrElse("bungee-plugin-message-channel", true);
        this.showPingRequests = config.getOrElse("show-ping-requests", false);
        this.failoverOnUnexpectedServerDisconnect = config
            .getOrElse("failover-on-unexpected-server-disconnect", true);
        this.announceProxyCommands = config.getOrElse("announce-proxy-commands", true);
        this.logCommandExecutions = config.getOrElse("log-command-executions", false);
        this.acceptTransfers = config.getOrElse("accepts-transfers", false);
        this.enableReusePort = config.getOrElse("enable-reuse-port", false);
        this.commandRateLimit = config.getIntOrElse("command-rate-limit", 50);
        this.forwardCommandsIfRateLimited = config.getOrElse("forward-commands-if-rate-limited", true);
        this.kickAfterRateLimitedCommands = config.getIntOrElse("kick-after-rate-limited-commands", 0);
        this.tabCompleteRateLimit = config.getIntOrElse("tab-complete-rate-limit", 10);
        this.kickAfterRateLimitedTabCompletes = config.getIntOrElse("kick-after-rate-limited-tab-completes", 0);
        this.allowIllegalCharactersInChat = config.getOrElse("allow-illegal-characters-in-chat", false);
        this.serverBrand = config.getOrElse("server-brand", "{backend-brand} ({proxy-brand})");
        this.fallbackVersionPing = config.getOrElse("fallback-version-ping", "{proxy-brand} {protocol-min}-{protocol-max}");
        this.alwaysFallBackPing = config.getOrElse("always-fallback-ping", false);
        this.proxyBrandCustom = config.getOrElse("custom-brand-proxy", "Velocity");
        this.backendBrandCustom = config.getOrElse("custom-brand-backend", "Paper");
      }

      this.serverBrandAsString = LegacyComponentSerializer.legacySection()
          .serialize(MiniMessage.miniMessage().deserialize(this.serverBrand));
      this.fallbackVersionPingAsString = LegacyComponentSerializer.legacySection()
          .serialize(MiniMessage.miniMessage().deserialize(this.fallbackVersionPing));
    }

    public boolean isCachePlayerProfileResultEnabled() {
      return this.cachePlayerProfileResult;
    }

    public int getProfileCacheExpiryMinutes() {
      return this.profileCacheExpiryMinutes;
    }

    public int getCompressionThreshold() {
      return compressionThreshold;
    }

    public int getCompressionLevel() {
      return compressionLevel;
    }

    public int getLoginRatelimit() {
      return loginRatelimit;
    }

    public int getConnectionTimeout() {
      return connectionTimeout;
    }

    public int getReadTimeout() {
      return readTimeout;
    }

    public boolean isProxyProtocol() {
      return proxyProtocol;
    }

    public void setProxyProtocol(final boolean proxyProtocol) {
      this.proxyProtocol = proxyProtocol;
    }

    public boolean isTcpFastOpen() {
      return tcpFastOpen;
    }

    public boolean isBungeePluginMessageChannel() {
      return bungeePluginMessageChannel;
    }

    public boolean isShowPingRequests() {
      return showPingRequests;
    }

    public boolean isFailoverOnUnexpectedServerDisconnect() {
      return failoverOnUnexpectedServerDisconnect;
    }

    public boolean isAnnounceProxyCommands() {
      return announceProxyCommands;
    }

    public boolean isLogCommandExecutions() {
      return logCommandExecutions;
    }

    public boolean isAcceptTransfers() {
      return this.acceptTransfers;
    }

    public boolean isEnableReusePort() {
      return enableReusePort;
    }

    public int getCommandRateLimit() {
      return commandRateLimit;
    }

    public boolean isForwardCommandsIfRateLimited() {
      return forwardCommandsIfRateLimited;
    }

    public int getKickAfterRateLimitedCommands() {
      return kickAfterRateLimitedCommands;
    }

    public int getTabCompleteRateLimit() {
      return tabCompleteRateLimit;
    }

    public int getKickAfterRateLimitedTabCompletes() {
      return kickAfterRateLimitedTabCompletes;
    }

    public boolean isAllowIllegalCharactersInChat() {
      return allowIllegalCharactersInChat;
    }

    public String getServerBrand() {
      return this.serverBrandAsString;
    }

    public String getFallbackVersionPing() {
      return this.fallbackVersionPingAsString;
    }

    public boolean getAlwaysFallBackPing() {
      return this.alwaysFallBackPing;
    }

    public String getProxyBrandCustom() {
      return this.proxyBrandCustom;
    }

    public String getBackendBrandCustom() {
      return this.backendBrandCustom;
    }

    @Override
    public String toString() {
      return "Advanced{"
          + "cachePlayerProfileResult=" + cachePlayerProfileResult
          + ", profileCacheExpiryMinutes=" + profileCacheExpiryMinutes
          + ", compressionThreshold=" + compressionThreshold
          + ", compressionLevel=" + compressionLevel
          + ", loginRatelimit=" + loginRatelimit
          + ", connectionTimeout=" + connectionTimeout
          + ", readTimeout=" + readTimeout
          + ", proxyProtocol=" + proxyProtocol
          + ", tcpFastOpen=" + tcpFastOpen
          + ", bungeePluginMessageChannel=" + bungeePluginMessageChannel
          + ", showPingRequests=" + showPingRequests
          + ", failoverOnUnexpectedServerDisconnect=" + failoverOnUnexpectedServerDisconnect
          + ", announceProxyCommands=" + announceProxyCommands
          + ", logCommandExecutions=" + logCommandExecutions
          + ", acceptTransfers=" + acceptTransfers
          + ", enableReusePort=" + enableReusePort
          + ", commandRateLimit=" + commandRateLimit
          + ", forwardCommandsIfRateLimited=" + forwardCommandsIfRateLimited
          + ", kickAfterRateLimitedCommands=" + kickAfterRateLimitedCommands
          + ", tabCompleteRateLimit=" + tabCompleteRateLimit
          + ", kickAfterRateLimitedTabCompletes=" + kickAfterRateLimitedTabCompletes
          + ", allowIllegalCharactersInChat=" + allowIllegalCharactersInChat
          + '}';
    }
  }

  private static final class Query {

    /**
     * Whether the legacy GameSpy query protocol is enabled.
     *
     * <p>This allows tools like server lists or query clients to gather basic server
     * metadata (name, player count, etc.) through UDP.</p>
     */
    @Expose
    private boolean queryEnabled = false;

    /**
     * The port on which the proxy will listen for query protocol requests.
     *
     * <p>This can be the same as or different from the main proxy port.</p>
     */
    @Expose
    private int queryPort = 25565;

    /**
     * The string returned as the "map name" in the query response.
     *
     * <p>Purely cosmetic, often shown as the server name in query-compatible tools.</p>
     */
    @Expose
    private String queryMap = "Velocity";

    /**
     * Whether the proxy should include plugin names in query responses.
     *
     * <p>This is often disabled to avoid leaking plugin information to the public.</p>
     */
    @Expose
    private boolean showPlugins = false;

    private Query() {
    }

    private Query(final boolean queryEnabled, final int queryPort, final String queryMap, final boolean showPlugins) {
      this.queryEnabled = queryEnabled;
      this.queryPort = queryPort;
      this.queryMap = queryMap;
      this.showPlugins = showPlugins;
    }

    private Query(final CommentedConfig config) {
      if (config != null) {
        this.queryEnabled = config.getOrElse("enabled", false);
        this.queryPort = config.getIntOrElse("port", 25565);
        this.queryMap = config.getOrElse("map", "Velocity");
        this.showPlugins = config.getOrElse("show-plugins", false);
      }
    }

    public boolean isQueryEnabled() {
      return queryEnabled;
    }

    public int getQueryPort() {
      return queryPort;
    }

    public String getQueryMap() {
      return queryMap;
    }

    public boolean shouldQueryShowPlugins() {
      return showPlugins;
    }

    @Override
    public String toString() {
      return "Query{"
          + "queryEnabled=" + queryEnabled
          + ", queryPort=" + queryPort
          + ", queryMap='" + queryMap + '\''
          + ", showPlugins=" + showPlugins
          + '}';
    }
  }

  /**
   * Configuration for metrics.
   */
  public static final class Metrics {

    /**
     * Whether metrics collection via bStats is enabled.
     * When enabled, Velocity will anonymously report usage statistics
     * such as player counts, Java version, and operating system to bStats.org.
     */
    private boolean enabled = true;

    private Metrics(final CommentedConfig toml) {
      if (toml != null) {
        this.enabled = toml.getOrElse("enabled", true);
      }
    }

    /**
     * Returns whether metrics reporting to bStats is enabled.
     *
     * @return true if metrics are enabled, false otherwise
     */
    public boolean isEnabled() {
      return enabled;
    }
  }

  /**
   * Redis configuration settings for the Velocity proxy.
   *
   * <p>This class provides the configuration options required to establish a connection
   * to a Redis server.
   * It supports settings for connection details (host, port),
   * authentication, SSL usage, and connection management parameters.
   * These settings
   * enable the Velocity proxy to leverage Redis for features such as data caching,
   * synchronization across multiple instances, and custom proxy functionalities.
   *
   * <p>The {@code Redis} configuration class includes options for:
   * <ul>
   * <li>Basic connection parameters, such as {@code host} and {@code port},
   * which specify the target Redis server.</li>
   * <li>Authentication details, including {@code username} and {@code password},
   * which are optional depending on the server's security configuration.</li>
   * <li>SSL support through {@code useSsl} for secure connections, especially
   * recommended for public or cloud-hosted Redis servers.</li>
   * <li>Connection management settings, such as {@code maxConcurrentConnections},
   * that control the number of parallel connections allowed.</li>
   * <li>Health check intervals via {@code pingIntervalMs} and timeout settings
   * for identifying unresponsive Redis connections or proxies.</li>
   * </ul>
   * Example usage might include using Redis to synchronize player data, manage
   * distributed cache, or coordinate proxy configurations in a multi-instance environment.
   */
  public static final class Redis {

    /**
     * Whether Redis support is enabled.
     * If true, Velocity will attempt to connect to a Redis server for multi-proxy coordination.
     */
    @Expose
    private boolean enabled;

    /**
     * The hostname or IP address of the Redis server to connect to.
     */
    @Expose
    private String host;

    /**
     * The port number to connect to on the Redis server.
     */
    @Expose
    private int port;

    /**
     * The optional username to authenticate with the Redis server.
     * This is only required if the Redis server uses ACLs.
     */
    @Expose
    private @Nullable String username;

    /**
     * The password to authenticate with the Redis server, if required.
     */
    @Expose
    private String password;

    /**
     * Whether to use SSL when connecting to the Redis server.
     */
    @Expose
    private boolean useSsl;

    /**
     * The maximum number of concurrent connections allowed to the Redis server.
     */
    @Expose
    private int maxConcurrentConnections;

    /**
     * The proxy ID to use when identifying this proxy to Redis.
     * If null, the proxy will operate anonymously.
     */
    @Expose
    private @Nullable String proxyId;

    private Redis(final CommentedConfig config) {
      if (config == null) {
        return;
      }

      this.enabled = config.getOrElse("enabled", false);
      this.host = config.getOrElse("host", "127.0.0.1");
      this.port = config.getOrElse("port", 6379);
      this.username = config.getOrElse("username", "");

      if (this.username.isEmpty()) {
        this.username = null;
      }

      this.password = config.get("password");
      this.useSsl = config.getOrElse("use-ssl", true);
      this.maxConcurrentConnections = config.getOrElse("max-concurrent-connections", 10);
      this.proxyId = config.get("proxy-id");

      if (this.proxyId == null || this.proxyId.isEmpty()) {
        this.proxyId = null;
      }
    }

    /**
     * Returns whether Redis integration is enabled.
     *
     * @return {@code true} if Redis is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Gets the Redis server hostname or IP address.
     *
     * @return the Redis host
     */
    public String getHost() {
      return host;
    }

    /**
     * Gets the Redis server port.
     *
     * @return the Redis port
     */
    public int getPort() {
      return port;
    }

    /**
     * Gets the Redis username for authentication, if configured.
     *
     * @return the Redis username, or {@code null} if not set
     */
    public @Nullable String getUsername() {
      return username;
    }

    /**
     * Gets the Redis password for authentication.
     *
     * @return the Redis password
     */
    public String getPassword() {
      return password;
    }

    /**
     * Returns whether SSL is used for connecting to Redis.
     *
     * @return {@code true} if SSL is used, {@code false} otherwise
     */
    public boolean isUseSsl() {
      return useSsl;
    }

    /**
     * Gets the maximum number of concurrent Redis connections allowed.
     *
     * @return the maximum number of connections
     */
    @Deprecated(forRemoval = true)
    public int getMaxConcurrentConnections() {
      return maxConcurrentConnections;
    }

    /**
     * Gets the proxy ID used to uniquely identify this instance in Redis,
     * or {@code null} if not explicitly configured.
     *
     * @return the proxy ID or {@code null}
     */
    public @Nullable String getProxyId() {
      return proxyId;
    }

    @Override
    public String toString() {
      return "Redis{"
          + "enabled=" + enabled
          + ", host=" + host
          + ", port=" + port
          + ", username=" + username
          // password excluded for security
          + ", useSsl=" + useSsl
          + ", maxConcurrentConnections=" + maxConcurrentConnections
          + '}';
    }
  }

  /**
   * Queue configuration data.
   */
  public static final class Queue {

    /**
     * Whether the queue system is enabled.
     */
    @Expose
    private boolean enabled;

    /**
     * A list of server names that should never use the queue system.
     */
    @Expose
    private List<String> noQueueServers;

    /**
     * If true, players are allowed to be in multiple queues simultaneously.
     */
    @Expose
    private boolean allowMultiQueue;

    /**
     * Delay in seconds before attempting to send the player to the target server.
     */
    @Expose
    private double sendDelay;

    /**
     * Delay in seconds before rechecking the queue position and updating players.
     */
    @Expose
    private double queueDelay;

    /**
     * Delay in seconds between sending queue position messages to players.
     */
    @Expose
    private double messageDelay;

    /**
     * Interval in seconds for pinging backend servers to determine availability.
     */
    @Expose
    private double backendPingInterval;

    /**
     * The maximum number of times the proxy should attempt to send a player before failing.
     */
    @Expose
    private int maxSendRetries;

    /**
     * If true, removes the player from the queue after successfully switching servers.
     */
    @Expose
    private boolean removePlayerOnServerSwitch;

    /**
     * If true, forwards the reason a player was kicked while waiting in queue.
     */
    @Expose
    private boolean forwardKickReason;

    /**
     * If true, allows players to join queues that are currently paused.
     */
    @Expose
    private boolean allowPausedQueueJoining;

    /**
     * If true, enables queue fallback during proxy shutdown to move players back to queue servers.
     */
    @Expose
    private boolean queueOnShutdown;

    /**
     * If true, allows the queue system to override BungeeCord plugin messaging behavior.
     */
    @Expose
    private boolean overrideBungeeMessaging;

    /**
     * Aliases that can be used by players to leave the queue (e.g., /leavequeue).
     */
    @Expose
    private List<String> leaveQueueAliases;

    /**
     * Aliases that allow admin interaction with the queue system (e.g., /queueadmin).
     */
    @Expose
    private List<String> queueAdminAliases;

    /**
     * List of proxy IDs that act as master proxies in a multi-proxy setup.
     */
    private List<String> masterProxyIds;

    /**
     * A list of reasons that will prevent a player from joining the queue.
     */
    private List<String> bannedReason;

    /**
     * A map of key-value server pairs for automatic queuing.
     */
    private Map<String, List<String>> autoQueueServers;

    private Queue(final CommentedConfig config) {
      if (config == null) {
        return;
      }

      this.enabled = config.getOrElse("enabled", false);
      this.noQueueServers = config.getOrElse("no-queue-servers", List.of());
      this.allowMultiQueue = config.getOrElse("allow-multi-queue", false);
      this.sendDelay = config.getOrElse("send-delay", 1.0);
      this.queueDelay = config.getOrElse("queue-delay", 0.0);
      this.messageDelay = config.getOrElse("message-delay", 1.0);
      this.backendPingInterval = config.getOrElse("backend-ping-interval", 5.0);
      this.maxSendRetries = config.getOrElse("max-send-retries", 10);
      this.removePlayerOnServerSwitch = config.getOrElse("remove-player-on-server-switch", true);
      this.forwardKickReason = config.getOrElse("forward-kick-reason", true);
      this.allowPausedQueueJoining = config.getOrElse("allow-paused-queue-joining", false);
      this.queueOnShutdown = config.getOrElse("queue-on-shutdown", true);
      this.overrideBungeeMessaging = config.getOrElse("override-bungee-messaging", true);
      this.leaveQueueAliases = config.getOrElse("leave-queue-aliases", new ArrayList<>());
      this.queueAdminAliases = config.getOrElse("queue-admin-aliases", new ArrayList<>());
      this.masterProxyIds = config.getOrElse("master-proxy-ids", new ArrayList<>());
      this.bannedReason = config.getOrElse("banned-reason", new ArrayList<>());
      this.autoQueueServers = parseAutoQueueServers(config.get("auto-queue-servers"));
    }

    private Map<String, List<String>> parseAutoQueueServers(CommentedConfig config) {
      Map<String, List<String>> autoQueueServers = new HashMap<>();
      for (UnmodifiableConfig.Entry entry : config.entrySet()) {
        String key = entry.getKey();

        if (entry.getValue() instanceof String) {
          String target = entry.getValue();
          autoQueueServers.put(key, ImmutableList.of(target));
        } else if (entry.getValue() instanceof List) {
          List<String> targets = entry.getValue();
          autoQueueServers.put(key, ImmutableList.copyOf(targets));
        } else {
          LOGGER.warn("Invalid value of type {} in auto queue servers!", entry.getValue().getClass());
        }
      }

      return ImmutableMap.copyOf(autoQueueServers);
    }

    /**
     * Returns whether the queue system is enabled.
     *
     * @return {@code true} if the queue system is enabled, {@code false} otherwise
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Returns whether players should be re-queued during proxy shutdown.
     *
     * @return {@code true} if queue fallback on shutdown is enabled, {@code false} otherwise
     */
    public boolean isQueueOnShutdown() {
      return queueOnShutdown;
    }

    /**
     * Returns whether players can join queues that are currently paused.
     *
     * @return {@code true} if paused queue joining is allowed, {@code false} otherwise
     */
    public boolean isAllowPausedQueueJoining() {
      return allowPausedQueueJoining;
    }

    /**
     * Gets the list of reasons that prevent a player from joining the queue.
     *
     * @return the list of banned reasons
     */
    public List<String> getBannedReason() {
      return this.bannedReason;
    }

    /**
     * Returns whether kick reasons are forwarded to players in the queue.
     *
     * @return {@code true} if kick reasons are forwarded, {@code false} otherwise
     */
    public boolean isForwardKickReason() {
      return forwardKickReason;
    }

    /**
     * Returns whether a player should be removed from the queue after switching servers.
     *
     * @return {@code true} if the player is removed on server switch, {@code false} otherwise
     */
    public boolean isRemovePlayerOnServerSwitch() {
      return removePlayerOnServerSwitch;
    }

    /**
     * Gets the maximum number of times a player can fail to connect before removal.
     *
     * @return the maximum number of connection retries
     */
    public int getMaxSendRetries() {
      return maxSendRetries;
    }

    /**
     * Gets the delay in seconds between sending position update messages to players in queue.
     *
     * @return the message delay in seconds
     */
    public double getMessageDelay() {
      return messageDelay;
    }

    /**
     * Gets the delay in seconds before attempting to send a player to their destination server.
     *
     * @return the send delay in seconds
     */
    public double getSendDelay() {
      return sendDelay;
    }

    /**
     * Gets the delay in seconds before the queue is processed again.
     *
     * @return the queue delay in seconds
     */
    public double getQueueDelay() {
      return this.queueDelay;
    }

    /**
     * Gets the interval in seconds between backend server pings.
     *
     * @return the backend ping interval in seconds
     */
    public double getBackendPingInterval() {
      return backendPingInterval;
    }

    /**
     * Returns whether players are allowed to join multiple queues simultaneously.
     *
     * @return {@code true} if multi-queue is enabled, {@code false} otherwise
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isAllowMultiQueue() {
      return allowMultiQueue;
    }

    /**
     * Gets the list of servers that are excluded from the queue system.
     *
     * @return a list of server names
     */
    public List<String> getNoQueueServers() {
      return noQueueServers == null ? Collections.emptyList() : noQueueServers;
    }

    /**
     * Returns whether this proxy should override BungeeCord plugin messaging behavior.
     *
     * @return {@code true} if override is enabled, {@code false} otherwise
     */
    public boolean shouldOverrideBungeeMessaging() {
      return overrideBungeeMessaging;
    }

    /**
     * Gets the list of command aliases that allow players to leave the queue.
     *
     * @return a list of leave queue command aliases
     */
    public List<String> getLeaveQueueAliases() {
      return leaveQueueAliases;
    }

    /**
     * Gets the list of command aliases for administrators managing the queue.
     *
     * @return a list of admin queue command aliases
     */
    public List<String> getQueueAdminAliases() {
      return queueAdminAliases;
    }

    /**
     * Gets the list of master proxy identifiers used in a multi-proxy environment.
     *
     * @return a list of master proxy IDs
     */
    public List<String> getMasterProxyIds() {
      return masterProxyIds;
    }

    /**
     * Gets a map of key-value server name pairs for auto-queueing.
     *
     * @return a map of key-value server name pairs.
     */
    public Map<String, List<String>> getAutoQueueServers() {
      return autoQueueServers;
    }

    @Override
    public String toString() {
      return "Queue{"
          + "enabled=" + enabled
          + ", allowPausedQueueJoining=" + allowPausedQueueJoining
          + ", forwardKickReason=" + forwardKickReason
          + ", removePlayerOnServerSwitch=" + removePlayerOnServerSwitch
          + ", maxSendRetries=" + maxSendRetries
          + ", messageDelay=" + messageDelay
          + ", backendPingInterval=" + backendPingInterval
          + ", sendDelay=" + sendDelay
          + ", queueDelay=" + queueDelay
          + ", allowMultiQueue=" + allowMultiQueue
          + ", noQueueServers=" + noQueueServers
          + ", overrideBungeeMessaging=" + overrideBungeeMessaging
          + ", leaveQueueAliases=" + leaveQueueAliases
          + ", autoQueueServers=" + autoQueueServers
          + ", queueAdminAliases=" + queueAdminAliases
          + ", masterProxyIds=" + masterProxyIds
          + ", bannedReason=" + bannedReason
          + '}';
    }
  }
}
