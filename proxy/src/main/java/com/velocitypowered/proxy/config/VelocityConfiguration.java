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
import com.velocitypowered.proxy.config.migration.TomlToYamlConverter;
import com.velocitypowered.proxy.util.AddressUtil;
import io.leangen.geantyref.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URL;
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
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.BlankLineStyle;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

/**
 * Velocity's configuration.
 */
public class VelocityConfiguration implements ProxyConfig {

  private static final Logger logger = LogManager.getLogger(VelocityConfiguration.class);

  /**
   * The current {@code velocity.yaml} version. Version 3 superseded the last TOML version, 2.9.
   */
  public static final int CONFIG_VERSION = 3;
  private static final String DEFAULT_RESOURCE = "default-velocity.yaml";
  private static final String LEGACY_CONFIG_FILE = "velocity.toml";
  private static final TypeToken<List<String>> STRING_LIST = new TypeToken<>() {};
  private static final String DEFAULT_FORWARDING_SECRET_FILE = "forwarding.secret";
  /**
   * Sections whose keys the user chooses. The shipped entries are examples, not defaults.
   */
  public static final Set<String> USER_NAMED_SECTIONS = Set.of("servers", "forced-hosts");

  @Expose
  private String bind;
  @Expose
  private String motd;
  @Expose
  private int showMaxPlayers;
  @Expose
  private boolean onlineMode;
  @Expose
  private boolean preventClientProxyConnections;
  @Expose
  private PlayerInfoForwarding playerInfoForwardingMode;
  private byte[] forwardingSecret;
  @Expose
  private boolean announceForge;
  @Expose
  private boolean onlineModeKickExistingPlayers;
  @Expose
  private PingPassthroughMode pingPassthrough;
  @Expose
  private boolean samplePlayersInPing;
  private final Servers servers;
  private final ForcedHosts forcedHosts;
  @Expose
  private final Advanced advanced;
  @Expose
  private final Query query;
  private final Metrics metrics;
  @Expose
  private boolean enablePlayerAddressLogging;
  private @MonotonicNonNull Component motdAsComponent;
  private @Nullable Favicon favicon;
  @Expose
  private boolean forceKeyAuthentication; // Added in 1.19
  @Expose
  private PacketLimiterConfig packetLimiterConfig;

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
  public Component getMotd() {
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
   * Reads the Velocity configuration from {@code path}. If there is no configuration there yet,
   * the legacy {@code velocity.toml} beside it is converted, or the shipped defaults are written.
   *
   * @param path the path to read from
   * @return the deserialized Velocity configuration
   * @throws IOException if we could not read from the {@code path}.
   */
  public static VelocityConfiguration read(Path path) throws IOException {
    final Path legacyPath = path.resolveSibling(LEGACY_CONFIG_FILE);
    final CommentedConfigurationNode defaults = loadDefaults();
    if (Files.isDirectory(path)) {
      throw new IOException(path + " is a directory, not a file. Docker creates a directory when "
          + "a file that does not exist on the host is mounted; create an empty file there first.");
    }
    // Written in place rather than through a temporary file moved over it, so a file mounted
    // into a container keeps working.
    final YamlConfigurationLoader loader = yamlLoader()
        .path(path)
        .sink(() -> Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        .build();

    if (Files.notExists(path) || Files.readString(path, StandardCharsets.UTF_8).isBlank()) {
      if (Files.exists(legacyPath)) {
        TomlToYamlConverter.convert(legacyPath, loader, defaults, logger);
      } else {
        // Create the forwarding-secret file on first-time startup if it doesn't exist
        final Path defaultForwardingSecretPath =
            path.resolveSibling(DEFAULT_FORWARDING_SECRET_FILE);
        if (Files.notExists(defaultForwardingSecretPath)) {
          Files.writeString(defaultForwardingSecretPath, generateRandomString(12));
        }
        try (InputStream shipped = defaultsUrl().openStream()) {
          Files.write(path, shipped.readAllBytes());
        }
      }
    } else if (Files.exists(legacyPath)) {
      logger.warn("Both {} and {} exist. Velocity only reads {}; remove {} once you no longer "
          + "need it.", path.getFileName(), legacyPath.getFileName(), path.getFileName(),
          legacyPath.getFileName());
    }

    final CommentedConfigurationNode config = loader.load();
    if (config.node("config-version").virtual()) {
      throw new IllegalStateException("Your " + path.getFileName() + " has no config-version.");
    }
    final int version = config.node("config-version").require(Integer.class);
    if (version != CONFIG_VERSION) {
      // Once velocity.yaml gets a new version, upgrade older files here with a
      // ConfigurationTransformation.versionedBuilder() keyed on config-version, and save them.
      throw new IllegalStateException("Your " + path.getFileName() + " is config-version "
          + version + ", but this version of Velocity only understands config-version "
          + CONFIG_VERSION + ".");
    }

    return read(withDefaults(config, defaults), path);
  }

  static VelocityConfiguration read(ConfigurationNode config, Path path) throws IOException {
    String forwardingSecretString = System.getenv().getOrDefault(
            "VELOCITY_FORWARDING_SECRET", "");
    if (forwardingSecretString.isBlank()) {
      final String forwardSecretFile =
          config.node("forwarding", "secret-file").require(String.class);
      final Path secretPath = path.resolveSibling(forwardSecretFile);
      if (Files.exists(secretPath)) {
        if (Files.isRegularFile(secretPath)) {
          forwardingSecretString = String.join("", Files.readAllLines(secretPath));
        } else {
          throw new RuntimeException(
                  "The file " + forwardSecretFile + " is not a valid file or it is a directory.");
        }
      } else {
        Files.createFile(secretPath);
        Files.writeString(secretPath, forwardingSecretString = generateRandomString(12),
            StandardCharsets.UTF_8);
        logger.info("The forwarding secret-file does not exist. A new file has been created at {}",
            forwardSecretFile);
      }
    }
    final byte[] forwardingSecret = forwardingSecretString.getBytes(StandardCharsets.UTF_8);
    final PlayerInfoForwarding forwardingMode =
        config.node("forwarding", "mode").require(PlayerInfoForwarding.class);

    // Throw an exception if the forwarding-secret file is empty and the proxy is using a
    // forwarding mode that requires it.
    if (forwardingSecret.length == 0
            && (forwardingMode == PlayerInfoForwarding.MODERN
            || forwardingMode == PlayerInfoForwarding.BUNGEEGUARD)) {
      throw new RuntimeException("The forwarding-secret file must not be empty.");
    }

    return new VelocityConfiguration(
            config.node("bind").require(String.class),
            config.node("motd").require(String.class),
            config.node("show-max-players").require(Integer.class),
            config.node("online-mode").require(Boolean.class),
            config.node("prevent-client-proxy-connections").require(Boolean.class),
            config.node("announce-forge").require(Boolean.class),
            forwardingMode,
            forwardingSecret,
            config.node("kick-existing-players").require(Boolean.class),
            PingPassthroughMode.fromConfig(config.node("ping-passthrough")),
            config.node("sample-players-in-ping").require(Boolean.class),
            config.node("logging", "player-addresses").require(Boolean.class),
            new Servers(config.node("servers"), config.node("try")),
            new ForcedHosts(config.node("forced-hosts")),
            new Advanced(config.node("advanced"), config.node("rate-limits"),
                config.node("logging")),
            new Query(config.node("query")),
            new Metrics(config.node("metrics")),
            config.node("force-key-authentication").require(Boolean.class),
            PacketLimiterConfig.fromConfig(config.node("packet-limiter"))
    );
  }

  /**
   * Builds a YAML loader that lays files out the way the shipped {@code default-velocity.yaml} is.
   *
   * @return the loader builder
   */
  public static YamlConfigurationLoader.Builder yamlLoader() {
    return YamlConfigurationLoader.builder()
        .indent(2)
        .nodeStyle(NodeStyle.BLOCK)
        .commentsEnabled(true)
        .blankLineStyle(BlankLineStyle.BEFORE_COMMENT)
        .headerMode(HeaderMode.NONE)
        .lineLength(Integer.MAX_VALUE)
        // A missing list must fail like any other missing option, not read as an empty one.
        .defaultOptions(options -> options.implicitInitialization(false));
  }

  static CommentedConfigurationNode loadDefaults() throws IOException {
    return yamlLoader().url(defaultsUrl()).build().load();
  }

  private static URL defaultsUrl() {
    final URL url = VelocityConfiguration.class.getClassLoader().getResource(DEFAULT_RESOURCE);
    if (url == null) {
      throw new IllegalStateException(DEFAULT_RESOURCE + " is missing from the classpath");
    }
    return url;
  }

  /**
   * Fills every option {@code config} leaves out from the shipped defaults. Sections whose keys
   * the user chooses are taken whole from {@code config}, never merged with the shipped examples.
   *
   * @param config the user's configuration
   * @param defaults the shipped defaults
   * @return the configuration to read
   */
  static ConfigurationNode withDefaults(ConfigurationNode config, ConfigurationNode defaults) {
    final ConfigurationNode fallback = defaults.copy();
    for (String section : USER_NAMED_SECTIONS) {
      if (!config.node(section).virtual()) {
        fallback.removeChild(section);
      }
    }
    return config.copy().mergeFrom(fallback);
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

    private Servers(ConfigurationNode section, ConfigurationNode attemptConnectionOrder)
        throws SerializationException {
      Map<String, String> servers = new HashMap<>();
      for (Map.Entry<Object, ? extends ConfigurationNode> entry : section.childrenMap().entrySet()) {
        servers.put(String.valueOf(entry.getKey()), entry.getValue().require(String.class));
      }
      this.servers = ImmutableMap.copyOf(servers);
      this.attemptConnectionOrder =
          ImmutableList.copyOf(attemptConnectionOrder.require(STRING_LIST));
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

    private ForcedHosts(ConfigurationNode section)
        throws SerializationException {
      Map<String, List<String>> forcedHosts = new HashMap<>();
      for (Map.Entry<Object, ? extends ConfigurationNode> entry : section.childrenMap().entrySet()) {
        forcedHosts.put(String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT),
            ImmutableList.copyOf(entry.getValue().require(STRING_LIST)));
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
    private int compressionThreshold;
    @Expose
    private int compressionLevel;
    @Expose
    private int loginRatelimit;
    @Expose
    private int connectionTimeout;
    @Expose
    private int readTimeout;
    @Expose
    private boolean proxyProtocol;
    @Expose
    private boolean tcpFastOpen;
    @Expose
    private boolean bungeePluginMessageChannel;
    @Expose
    private boolean showPingRequests;
    @Expose
    private boolean failoverOnUnexpectedServerDisconnect;
    @Expose
    private boolean announceProxyCommands;
    @Expose
    private boolean logCommandExecutions;
    @Expose
    private boolean logPlayerConnections;
    @Expose
    private boolean acceptTransfers;
    @Expose
    private boolean enableReusePort;
    @Expose
    private int commandRateLimit;
    @Expose
    private boolean forwardCommandsIfRateLimited;
    @Expose
    private int kickAfterRateLimitedCommands;
    @Expose
    private int tabCompleteRateLimit;
    @Expose
    private int kickAfterRateLimitedTabCompletes;

    private Advanced(ConfigurationNode section, ConfigurationNode rateLimits,
        ConfigurationNode logging) throws SerializationException {
      this.compressionThreshold = section.node("compression-threshold").require(Integer.class);
      this.compressionLevel = section.node("compression-level").require(Integer.class);
      this.loginRatelimit = rateLimits.node("login").require(Integer.class);
      this.connectionTimeout = section.node("connection-timeout").require(Integer.class);
      this.readTimeout = section.node("read-timeout").require(Integer.class);
      this.proxyProtocol = section.node("haproxy-protocol").require(Boolean.class);
      this.tcpFastOpen = section.node("tcp-fast-open").require(Boolean.class);
      this.bungeePluginMessageChannel =
          section.node("bungee-plugin-message-channel").require(Boolean.class);
      this.showPingRequests = logging.node("ping-requests").require(Boolean.class);
      this.failoverOnUnexpectedServerDisconnect =
          section.node("failover-on-unexpected-server-disconnect").require(Boolean.class);
      this.announceProxyCommands = section.node("announce-proxy-commands").require(Boolean.class);
      this.logCommandExecutions = logging.node("commands").require(Boolean.class);
      this.logPlayerConnections = logging.node("player-connections").require(Boolean.class);
      this.acceptTransfers = section.node("accepts-transfers").require(Boolean.class);
      this.enableReusePort = section.node("enable-reuse-port").require(Boolean.class);
      this.commandRateLimit = rateLimits.node("commands").require(Integer.class);
      this.forwardCommandsIfRateLimited =
          rateLimits.node("forward-commands-if-limited").require(Boolean.class);
      this.kickAfterRateLimitedCommands = rateLimits.node("kick-after-commands").require(Integer.class);
      this.tabCompleteRateLimit = rateLimits.node("tab-completes").require(Integer.class);
      this.kickAfterRateLimitedTabCompletes =
          rateLimits.node("kick-after-tab-completes").require(Integer.class);
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

    private Query(ConfigurationNode section)
        throws SerializationException {
      this.queryEnabled = section.node("enabled").require(Boolean.class);
      this.queryPort = section.node("port").require(Integer.class);
      this.queryMap = section.node("map").require(String.class);
      this.showPlugins = section.node("show-plugins").require(Boolean.class);
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

    private Metrics(ConfigurationNode section)
        throws SerializationException {
      this.enabled = section.node("enabled").require(Boolean.class);
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
     * Returns the PacketLimiterConfig in a config section.
     *
     * @param section the configuration section to parse
     * @return the packet limiter config
     */
    public static PacketLimiterConfig fromConfig(ConfigurationNode section)
        throws SerializationException {
      return new PacketLimiterConfig(
          section.node("interval").require(Integer.class),
          section.node("packets-per-second").require(Integer.class),
          section.node("bytes-per-second").require(Integer.class),
          section.node("decompressed-bytes-per-second").require(Integer.class)
      );
    }
  }
}
