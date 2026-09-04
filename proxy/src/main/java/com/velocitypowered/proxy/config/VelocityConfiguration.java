/*
 * Copyright (C) 2018-2023 Velocity Contributors
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.Expose;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.config.migration.ConfigurationMigration;
import com.velocitypowered.proxy.config.migration.TomlToYamlConverter;
import com.velocitypowered.proxy.util.AddressUtil;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Velocity's configuration.
 */
public class VelocityConfiguration implements ProxyConfig {

  private static final Logger logger = LogManager.getLogger(VelocityConfiguration.class);
  private static final String DEFAULT_CONFIGURATION_RESOURCE = "default-velocity.yml";
  private static final String LEGACY_CONFIGURATION_FILE = "velocity.toml";
  private static final String DEFAULT_FORWARDING_SECRET_FILE = "forwarding.secret";

  @Expose
  private final String bind;
  @Expose
  private final String motd;
  @Expose
  private final int showMaxPlayers;
  @Expose
  private final boolean onlineMode;
  @Expose
  private final boolean preventClientProxyConnections;
  @Expose
  private final PlayerInfoForwarding playerInfoForwardingMode;
  private final byte[] forwardingSecret;
  @Expose
  private final boolean announceForge;
  @Expose
  private final boolean onlineModeKickExistingPlayers;
  @Expose
  private final PingPassthroughMode pingPassthrough;
  @Expose
  private final boolean samplePlayersInPing;
  private final Servers servers;
  private final ForcedHosts forcedHosts;
  @Expose
  private final Advanced advanced;
  @Expose
  private final Query query;
  private final Metrics metrics;
  @Expose
  private final boolean enablePlayerAddressLogging;
  private net.kyori.adventure.text.@MonotonicNonNull Component motdAsComponent;
  private @Nullable Favicon favicon;
  @Expose
  private final boolean forceKeyAuthentication; // Added in 1.19
  @Expose
  private final PacketLimiterConfig packetLimiterConfig;

  private VelocityConfiguration(String bind, String motd, int showMaxPlayers, boolean onlineMode,
      boolean preventClientProxyConnections, boolean announceForge,
      PlayerInfoForwarding playerInfoForwardingMode, byte[] forwardingSecret,
      boolean onlineModeKickExistingPlayers, PingPassthroughMode pingPassthrough,
      boolean samplePlayersInPing, boolean enablePlayerAddressLogging, Servers servers,
      ForcedHosts forcedHosts, Advanced advanced, Query query, Metrics metrics,
      boolean forceKeyAuthentication, PacketLimiterConfig packetLimiterConfig) {
    this.bind = bind;
    this.motd = motd;
    this.showMaxPlayers = showMaxPlayers;
    this.onlineMode = onlineMode;
    this.preventClientProxyConnections = preventClientProxyConnections;
    this.announceForge = announceForge;
    this.playerInfoForwardingMode = playerInfoForwardingMode;
    this.forwardingSecret = forwardingSecret;
    this.onlineModeKickExistingPlayers = onlineModeKickExistingPlayers;
    this.pingPassthrough = pingPassthrough;
    this.samplePlayersInPing = samplePlayersInPing;
    this.enablePlayerAddressLogging = enablePlayerAddressLogging;
    this.servers = servers;
    this.forcedHosts = forcedHosts;
    this.advanced = advanced;
    this.query = query;
    this.metrics = metrics;
    this.forceKeyAuthentication = forceKeyAuthentication;
    this.packetLimiterConfig = packetLimiterConfig;
  }

  /**
   * Attempts to validate the configuration.
   *
   * @return {@code true} if the configuration is sound, {@code false} if not
   */
  public boolean validate() {
    boolean valid = true;

    if (bind.isEmpty()) {
      logger.error("'bind' option is empty.");
      valid = false;
    } else {
      try {
        AddressUtil.parseAddress(bind);
      } catch (IllegalArgumentException e) {
        logger.error("'bind' option does not specify a valid IP address.", e);
        valid = false;
      }
    }

    if (!onlineMode) {
      logger.warn("The proxy is running in offline mode! This is a security risk and you will NOT "
          + "receive any support!");
    }

    switch (playerInfoForwardingMode) {
      case NONE -> logger.warn("Player info forwarding is disabled! All players will appear to be connecting "
            + "from the proxy and will have offline-mode UUIDs.");
      case MODERN, BUNGEEGUARD -> {
        if (forwardingSecret == null || forwardingSecret.length == 0) {
          logger.error("You don't have a forwarding secret set. This is required for security.");
          valid = false;
        }
      }
      default -> {
      }
    }

    if (servers.getServers().isEmpty()) {
      logger.warn("You don't have any servers configured.");
    }

    for (Map.Entry<String, String> entry : servers.getServers().entrySet()) {
      try {
        AddressUtil.parseAddress(entry.getValue());
      } catch (IllegalArgumentException e) {
        logger.error("Server {} does not have a valid IP address.", entry.getKey(), e);
        valid = false;
      }
    }

    for (String s : servers.getAttemptConnectionOrder()) {
      if (!servers.getServers().containsKey(s)) {
        logger.error("Fallback server " + s + " is not registered in your configuration!");
        valid = false;
      }
    }

    for (Map.Entry<String, List<String>> entry : forcedHosts.getForcedHosts().entrySet()) {
      if (entry.getValue().isEmpty()) {
        logger.error("Forced host '{}' does not contain any servers", entry.getKey());
        valid = false;
        continue;
      }

      for (String server : entry.getValue()) {
        if (!servers.getServers().containsKey(server)) {
          logger.error("Server '{}' for forced host '{}' does not exist", server, entry.getKey());
          valid = false;
        }
      }
    }

    try {
      getMotd();
    } catch (Exception e) {
      logger.error("Can't parse your MOTD", e);
      valid = false;
    }

    if (advanced.compressionLevel < -1 || advanced.compressionLevel > 9) {
      logger.error("Invalid compression level {}", advanced.compressionLevel);
      valid = false;
    } else if (advanced.compressionLevel == 0) {
      logger.warn("ALL packets going through the proxy will be uncompressed. This will increase "
          + "bandwidth usage.");
    }

    if (advanced.compressionThreshold < -1) {
      logger.error("Invalid compression threshold {}", advanced.compressionLevel);
      valid = false;
    } else if (advanced.compressionThreshold == 0) {
      logger.warn("ALL packets going through the proxy will be compressed. This will compromise "
          + "throughput and increase CPU usage!");
    }

    if (advanced.loginRatelimit < 0) {
      logger.error("Invalid login ratelimit {}ms", advanced.loginRatelimit);
      valid = false;
    }

    if (advanced.commandRateLimit < 0) {
      logger.error("Invalid command rate limit {}", advanced.commandRateLimit);
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
        logger.info("Unable to load your server-icon.png, continuing without it.", e);
      }
    }
  }

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
  public net.kyori.adventure.text.Component getMotd() {
    if (motdAsComponent == null) {
      motdAsComponent = MiniMessage.miniMessage().deserialize(motd);
    }
    return motdAsComponent;
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
  public boolean shouldPreventClientProxyConnections() {
    return preventClientProxyConnections;
  }

  public PlayerInfoForwarding getPlayerInfoForwardingMode() {
    return playerInfoForwardingMode;
  }

  public byte[] getForwardingSecret() {
    return forwardingSecret.clone();
  }

  @Override
  public Map<String, String> getServers() {
    return servers.getServers();
  }

  @Override
  public List<String> getAttemptConnectionOrder() {
    return servers.getAttemptConnectionOrder();
  }

  @Override
  public Map<String, List<String>> getForcedHosts() {
    return forcedHosts.getForcedHosts();
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

  public boolean isProxyProtocol() {
    return advanced.isProxyProtocol();
  }

  public void setProxyProtocol(boolean proxyProtocol) {
    advanced.setProxyProtocol(proxyProtocol);
  }

  public boolean useTcpFastOpen() {
    return advanced.isTcpFastOpen();
  }

  public Metrics getMetrics() {
    return metrics;
  }

  public PingPassthroughMode getPingPassthrough() {
    return pingPassthrough;
  }

  public boolean getSamplePlayersInPing() {
    return samplePlayersInPing;
  }

  public boolean isPlayerAddressLoggingEnabled() {
    return enablePlayerAddressLogging;
  }

  public boolean isBungeePluginChannelEnabled() {
    return advanced.isBungeePluginMessageChannel();
  }

  public boolean isShowPingRequests() {
    return advanced.isShowPingRequests();
  }

  public boolean isFailoverOnUnexpectedServerDisconnect() {
    return advanced.isFailoverOnUnexpectedServerDisconnect();
  }

  public boolean isAnnounceProxyCommands() {
    return advanced.isAnnounceProxyCommands();
  }

  public boolean isLogCommandExecutions() {
    return advanced.isLogCommandExecutions();
  }

  public boolean isLogPlayerConnections() {
    return advanced.isLogPlayerConnections();
  }

  public boolean isAcceptTransfers() {
    return this.advanced.isAcceptTransfers();
  }

  public boolean isForceKeyAuthentication() {
    return forceKeyAuthentication;
  }

  public boolean isEnableReusePort() {
    return advanced.isEnableReusePort();
  }

  public PacketLimiterConfig getPacketLimiterConfig() {
    return packetLimiterConfig;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("bind", bind)
        .add("motd", motd)
        .add("showMaxPlayers", showMaxPlayers)
        .add("onlineMode", onlineMode)
        .add("playerInfoForwardingMode", playerInfoForwardingMode)
        .add("forwardingSecret", forwardingSecret)
        .add("announceForge", announceForge)
        .add("servers", servers)
        .add("forcedHosts", forcedHosts)
        .add("advanced", advanced)
        .add("query", query)
        .add("favicon", favicon)
        .add("enablePlayerAddressLogging", enablePlayerAddressLogging)
        .add("forceKeyAuthentication", forceKeyAuthentication)
        .add("packetLimiterConfig", packetLimiterConfig)
        .toString();
  }

  /**
   * Reads the Velocity configuration from {@code path}, first creating it from the shipped
   * defaults, or converting a legacy TOML configuration beside it, if it does not exist yet.
   */
  public static VelocityConfiguration read(Path path) throws IOException {
    final Path legacyPath = path.resolveSibling(LEGACY_CONFIGURATION_FILE);
    final Path defaultForwardingSecretPath = path.resolveSibling(DEFAULT_FORWARDING_SECRET_FILE);
    if (Files.notExists(path) && Files.notExists(legacyPath)
        && Files.notExists(defaultForwardingSecretPath)) {
      Files.writeString(defaultForwardingSecretPath, generateRandomString(12));
    }

    final ConfigurationDocument defaults = defaultConfiguration();

    boolean changed = false;
    if (Files.notExists(path)) {
      if (Files.exists(legacyPath)) {
        TomlToYamlConverter.convert(legacyPath, path, logger);
        changed = true;
      } else {
        try (InputStream shipped = defaultConfigurationStream()) {
          Files.copy(shipped, path);
        }
      }
    }

    final ConfigurationDocument document = YamlDocument.load(path);
    final int version = ConfigurationMigration.versionOf(document);
    final int currentVersion = ConfigurationMigration.versionOf(defaults);
    if (version > currentVersion) {
      throw new IllegalStateException("Your configuration is version " + version + ", but this "
          + "version of Velocity only understands up to version " + currentVersion + ".");
    }

    final ConfigurationMigration[] migrations = {
    };
    for (final ConfigurationMigration migration : migrations) {
      if (migration.shouldMigrate(document)) {
        migration.migrate(document, logger);
        changed = true;
      }
    }

    if (changed) {
      document.copyCommentsFrom(defaults);
      document.save(path);
    }

    return load(new LayeredConfiguration(document, defaults), path);
  }

  /**
   * Reads the shipped configuration, which defines every option and so backs every read.
   */
  public static ConfigurationDocument defaultConfiguration() throws IOException {
    try (Reader reader = new InputStreamReader(defaultConfigurationStream(),
        StandardCharsets.UTF_8)) {
      return YamlDocument.read(reader);
    }
  }

  private static InputStream defaultConfigurationStream() {
    final InputStream stream = VelocityConfiguration.class.getClassLoader()
        .getResourceAsStream(DEFAULT_CONFIGURATION_RESOURCE);
    if (stream == null) {
      throw new IllegalStateException("Default configuration file does not exist.");
    }
    return stream;
  }

  static VelocityConfiguration load(Configuration config, Path path) throws IOException {
    String forwardingSecretString = System.getenv().getOrDefault(
            "VELOCITY_FORWARDING_SECRET", "");
    if (forwardingSecretString.isBlank()) {
      final Path secretPath = path.resolveSibling(config.getString("forwarding-secret-file"));
      if (Files.exists(secretPath)) {
        if (Files.isRegularFile(secretPath)) {
          forwardingSecretString = String.join("", Files.readAllLines(secretPath));
        } else {
          throw new RuntimeException(
                  "The file " + secretPath + " is not a valid file or it is a directory.");
        }
      } else {
        Files.createFile(secretPath);
        Files.writeString(secretPath, forwardingSecretString = generateRandomString(12),
            StandardCharsets.UTF_8);
        logger.info("The forwarding-secret-file does not exist. A new file has been created at {}",
            secretPath);
      }
    }
    final byte[] forwardingSecret = forwardingSecretString.getBytes(StandardCharsets.UTF_8);
    final PlayerInfoForwarding forwardingMode = config.getEnum(
            "player-info-forwarding-mode", PlayerInfoForwarding.class);

    if (forwardingSecret.length == 0
            && (forwardingMode == PlayerInfoForwarding.MODERN
            || forwardingMode == PlayerInfoForwarding.BUNGEEGUARD)) {
      throw new RuntimeException("The forwarding-secret file must not be empty.");
    }

    return new VelocityConfiguration(
            config.getString("bind"),
            config.getString("motd"),
            config.getInt("show-max-players"),
            config.getBoolean("online-mode"),
            config.getBoolean("prevent-client-proxy-connections"),
            config.getBoolean("announce-forge"),
            forwardingMode,
            forwardingSecret,
            config.getBoolean("kick-existing-players"),
            PingPassthroughMode.fromConfig(config),
            config.getBoolean("sample-players-in-ping"),
            config.getBoolean("enable-player-address-logging"),
            new Servers(config),
            new ForcedHosts(config),
            new Advanced(config),
            new Query(config),
            new Metrics(config),
            config.getBoolean("force-key-authentication"),
            PacketLimiterConfig.fromConfig(config)
    );
  }

  /**
   * Generates a Random String.
   *
   * @param length the required string size.
   * @return a new random string.
   */
  public static String generateRandomString(int length) {
    final String chars = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
    final StringBuilder builder = new StringBuilder();
    final Random rnd = new SecureRandom();
    for (int i = 0; i < length; i++) {
      builder.append(chars.charAt(rnd.nextInt(chars.length())));
    }
    return builder.toString();
  }

  public boolean isOnlineModeKickExistingPlayers() {
    return onlineModeKickExistingPlayers;
  }

  private static class Servers {

    private final Map<String, String> servers;
    private final List<String> attemptConnectionOrder;

    private Servers(Configuration config) {
      final Configuration section = config.getSection("servers");
      Map<String, String> servers = new HashMap<>();
      for (String name : section.keys()) {
        if (!name.equalsIgnoreCase("try")) {
          servers.put(name, section.getString(name));
        }
      }
      this.servers = ImmutableMap.copyOf(servers);
      this.attemptConnectionOrder = ImmutableList.copyOf(config.getStringList("servers.try"));
    }

    private Map<String, String> getServers() {
      return servers;
    }

    public List<String> getAttemptConnectionOrder() {
      return attemptConnectionOrder;
    }

    @Override
    public String toString() {
      return "Servers{"
          + "servers=" + servers
          + ", attemptConnectionOrder=" + attemptConnectionOrder
          + '}';
    }
  }

  private static class ForcedHosts {

    private final Map<String, List<String>> forcedHosts;

    private ForcedHosts(Configuration config) {
      final Configuration section = config.getSection("forced-hosts");
      Map<String, List<String>> forcedHosts = new HashMap<>();
      for (String host : section.keys()) {
        forcedHosts.put(host.toLowerCase(Locale.ROOT),
            ImmutableList.copyOf(section.getStringList(host)));
      }
      this.forcedHosts = ImmutableMap.copyOf(forcedHosts);
    }

    private Map<String, List<String>> getForcedHosts() {
      return forcedHosts;
    }

    @Override
    public String toString() {
      return "ForcedHosts{"
          + "forcedHosts=" + forcedHosts
          + '}';
    }
  }

  private static class Advanced {

    @Expose
    private final int compressionThreshold;
    @Expose
    private final int compressionLevel;
    @Expose
    private final int loginRatelimit;
    @Expose
    private final int connectionTimeout;
    @Expose
    private final int readTimeout;
    @Expose
    private boolean proxyProtocol;
    @Expose
    private final boolean tcpFastOpen;
    @Expose
    private final boolean bungeePluginMessageChannel;
    @Expose
    private final boolean showPingRequests;
    @Expose
    private final boolean failoverOnUnexpectedServerDisconnect;
    @Expose
    private final boolean announceProxyCommands;
    @Expose
    private final boolean logCommandExecutions;
    @Expose
    private final boolean logPlayerConnections;
    @Expose
    private final boolean acceptTransfers;
    @Expose
    private final boolean enableReusePort;
    @Expose
    private final int commandRateLimit;
    @Expose
    private final boolean forwardCommandsIfRateLimited;
    @Expose
    private final int kickAfterRateLimitedCommands;
    @Expose
    private final int tabCompleteRateLimit;
    @Expose
    private final int kickAfterRateLimitedTabCompletes;

    private Advanced(Configuration config) {
      this.compressionThreshold = config.getInt("advanced.compression-threshold");
      this.compressionLevel = config.getInt("advanced.compression-level");
      this.loginRatelimit = config.getInt("advanced.login-ratelimit");
      this.connectionTimeout = config.getInt("advanced.connection-timeout");
      this.readTimeout = config.getInt("advanced.read-timeout");
      this.proxyProtocol = config.getBoolean("advanced.haproxy-protocol");
      this.tcpFastOpen = config.getBoolean("advanced.tcp-fast-open");
      this.bungeePluginMessageChannel = config.getBoolean("advanced.bungee-plugin-message-channel");
      this.showPingRequests = config.getBoolean("advanced.show-ping-requests");
      this.failoverOnUnexpectedServerDisconnect =
          config.getBoolean("advanced.failover-on-unexpected-server-disconnect");
      this.announceProxyCommands = config.getBoolean("advanced.announce-proxy-commands");
      this.logCommandExecutions = config.getBoolean("advanced.log-command-executions");
      this.logPlayerConnections = config.getBoolean("advanced.log-player-connections");
      this.acceptTransfers = config.getBoolean("advanced.accepts-transfers");
      this.enableReusePort = config.getBoolean("advanced.enable-reuse-port");
      this.commandRateLimit = config.getInt("advanced.command-rate-limit");
      this.forwardCommandsIfRateLimited =
          config.getBoolean("advanced.forward-commands-if-rate-limited");
      this.kickAfterRateLimitedCommands =
          config.getInt("advanced.kick-after-rate-limited-commands");
      this.tabCompleteRateLimit = config.getInt("advanced.tab-complete-rate-limit");
      this.kickAfterRateLimitedTabCompletes =
          config.getInt("advanced.kick-after-rate-limited-tab-completes");
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

    public void setProxyProtocol(boolean proxyProtocol) {
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

    public boolean isLogPlayerConnections() {
      return logPlayerConnections;
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

    @Override
    public String toString() {
      return "Advanced{"
          + "compressionThreshold=" + compressionThreshold
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
          + ", logPlayerConnections=" + logPlayerConnections
          + ", acceptTransfers=" + acceptTransfers
          + ", enableReusePort=" + enableReusePort
          + '}';
    }
  }

  private static class Query {

    @Expose
    private final boolean queryEnabled;
    @Expose
    private final int queryPort;
    @Expose
    private final String queryMap;
    @Expose
    private final boolean showPlugins;

    private Query(Configuration config) {
      this.queryEnabled = config.getBoolean("query.enabled");
      this.queryPort = config.getInt("query.port");
      this.queryMap = config.getString("query.map");
      this.showPlugins = config.getBoolean("query.show-plugins");
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
  public static class Metrics {

    private final boolean enabled;

    private Metrics(Configuration config) {
      this.enabled = config.getBoolean("metrics.enabled");
    }

    public boolean isEnabled() {
      return enabled;
    }
  }

  /**
   * Configuration for packet limiting.
   *
   * @param interval                the interval in seconds to measure packets over
   * @param pps                     the maximum number of packets per second allowed
   * @param bytes                   the maximum number of bytes per second allowed
   * @param bytesAfterDecompression the maximum number of decompressed bytes per second allowed
   */
  public record PacketLimiterConfig(int interval, int pps, int bytes, int bytesAfterDecompression) {
    public static PacketLimiterConfig DEFAULT = new PacketLimiterConfig(7, -1, -1, 5242880);

    /**
     * Returns the packet limiter configuration.
     */
    public static PacketLimiterConfig fromConfig(Configuration config) {
      return new PacketLimiterConfig(
          config.getInt("packet-limiter.interval"),
          config.getInt("packet-limiter.packets-per-second"),
          config.getInt("packet-limiter.bytes-per-second"),
          config.getInt("packet-limiter.decompressed-bytes-per-second")
      );
    }
  }
}
