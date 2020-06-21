package com.velocitypowered.proxy.config;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.util.AddressUtil;
import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.kyori.text.Component;
import net.kyori.text.serializer.gson.GsonComponentSerializer;
import net.kyori.text.serializer.legacy.LegacyComponentSerializer;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class VelocityConfiguration extends AnnotatedConfig implements ProxyConfig {

  @Comment("Config version. Do not change this")
  @ConfigKey("config-version")
  private final String configVersion = "1.0";

  @Comment("What port should the proxy be bound to? By default, we'll bind to all addresses on"
      + " port 25577.")
  private String bind = "0.0.0.0:25577";

  @Comment({"What should be the MOTD? This gets displayed when the player adds your server to",
      "their server list. Legacy color codes and JSON are accepted."})
  private String motd = "&3A Velocity Server";

  @Comment({
      "What should we display for the maximum number of players? (Velocity does not support a cap",
      "on the number of players online.)"
  })
  @ConfigKey("show-max-players")
  private int showMaxPlayers = 500;

  @Comment("Should we authenticate players with Mojang? By default, this is on.")
  @ConfigKey("online-mode")
  private boolean onlineMode = true;

  @Comment({
      "If client's ISP/AS sent from this proxy is different from the one from Mojang's",
      "authentication server, the player is kicked. This disallows some VPN and proxy",
      "connections but is a weak form of protection."
  })
  @ConfigKey("prevent-client-proxy-connections")
  private boolean preventClientProxyConnections = false;

  @Comment({
      "Should we forward IP addresses and other data to backend servers?",
      "Available options:",
      "- \"none\":        No forwarding will be done. All players will appear to be connecting",
      "                 from the proxy and will have offline-mode UUIDs.",
      "- \"legacy\":      Forward player IPs and UUIDs in a BungeeCord-compatible format. Use this",
      "                 if you run servers using Minecraft 1.12 or lower.",
      "- \"bungeeguard\": Forward player IPs and UUIDs in a format supported by the BungeeGuard",
      "                 plugin. Use this if you run servers using Minecraft 1.12 or lower, and are",
      "                 unable to implement network level firewalling (on a shared host).",
      "- \"modern\":      Forward player IPs and UUIDs as part of the login process using",
      "                 Velocity's native forwarding. Only applicable for Minecraft 1.13 or higher."
  })
  @ConfigKey("player-info-forwarding-mode")
  private PlayerInfoForwarding playerInfoForwardingMode = PlayerInfoForwarding.NONE;

  @StringAsBytes
  @Comment("If you are using modern or BungeeGuard IP forwarding, configure an unique secret here.")
  @ConfigKey("forwarding-secret")
  private byte[] forwardingSecret = generateRandomString(12).getBytes(StandardCharsets.UTF_8);

  @Comment({
      "Announce whether or not your server supports Forge. If you run a modded server, we",
      "suggest turning this on.",
      "",
      "If your network runs one modpack consistently, consider using ping-passthrough = \"mods\"",
      "instead for a nicer display in the server list."
  })
  @ConfigKey("announce-forge")
  private boolean announceForge = false;

  @Comment({"If enabled (default is false) and the proxy is in online mode, Velocity will kick",
      "any existing player who is online if a duplicate connection attempt is made."})
  @ConfigKey("kick-existing-players")
  private boolean onlineModeKickExistingPlayers = false;

  @Comment({
      "Should Velocity pass server list ping requests to a backend server?",
      "Available options:",
      "- \"disabled\":    No pass-through will be done. The velocity.toml and server-icon.png",
      "                 will determine the initial server list ping response.",
      "- \"mods\":        Passes only the mod list from your backend server into the response.",
      "                 The first server in your try list (or forced host) with a mod list will be",
      "                 used. If no backend servers can be contacted, Velocity won't display any",
      "                 mod information.",
      "- \"description\": Uses the description and mod list from the backend server. The first",
      "                 server in the try (or forced host) list that responds is used for the",
      "                 description and mod list.",
      "- \"all\":         Uses the backend server's response as the proxy response. The Velocity",
      "                 configuration is used if no servers could be contacted."
  })
  @ConfigKey("ping-passthrough")
  private PingPassthroughMode pingPassthrough = PingPassthroughMode.DISABLED;

  @Table("[servers]")
  private final Servers servers;

  @Table("[forced-hosts]")
  private final ForcedHosts forcedHosts;

  @Table("[advanced]")
  private final Advanced advanced;

  @Table("[query]")
  private final Query query;

  @Table("[metrics]")
  private final Metrics metrics;

  @Ignore
  private @MonotonicNonNull Component motdAsComponent;

  @Ignore
  private @Nullable Favicon favicon;

  private VelocityConfiguration(Servers servers, ForcedHosts forcedHosts, Advanced advanced,
      Query query, Metrics metrics) {
    this.servers = servers;
    this.forcedHosts = forcedHosts;
    this.advanced = advanced;
    this.query = query;
    this.metrics = metrics;
  }

  private VelocityConfiguration(String bind, String motd, int showMaxPlayers, boolean onlineMode,
      boolean announceForge, PlayerInfoForwarding playerInfoForwardingMode, byte[] forwardingSecret,
      boolean onlineModeKickExistingPlayers, PingPassthroughMode pingPassthrough, Servers servers,
      ForcedHosts forcedHosts, Advanced advanced, Query query, Metrics metrics) {
    this.bind = bind;
    this.motd = motd;
    this.showMaxPlayers = showMaxPlayers;
    this.onlineMode = onlineMode;
    this.announceForge = announceForge;
    this.playerInfoForwardingMode = playerInfoForwardingMode;
    this.forwardingSecret = forwardingSecret;
    this.onlineModeKickExistingPlayers = onlineModeKickExistingPlayers;
    this.pingPassthrough = pingPassthrough;
    this.servers = servers;
    this.forcedHosts = forcedHosts;
    this.advanced = advanced;
    this.query = query;
    this.metrics = metrics;
  }

  /**
   * Attempts to validate the configuration.
   * @return {@code true} if the configuration is sound, {@code false} if not
   */
  public boolean validate() {
    boolean valid = true;
    Logger logger = AnnotatedConfig.getLogger();

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
      logger.info("Proxy is running in offline mode!");
    }

    switch (playerInfoForwardingMode) {
      case NONE:
        logger.warn("Player info forwarding is disabled! All players will appear to be connecting "
            + "from the proxy and will have offline-mode UUIDs.");
        break;
      case MODERN:
      case BUNGEEGUARD:
        if (forwardingSecret == null || forwardingSecret.length == 0) {
          logger.error("You don't have a forwarding secret set. This is required for security.");
          valid = false;
        }
        break;
      default:
        break;
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
      getMotdComponent();
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

    loadFavicon();

    return valid;
  }

  private void loadFavicon() {
    Path faviconPath = Paths.get("server-icon.png");
    if (Files.exists(faviconPath)) {
      try {
        this.favicon = Favicon.create(faviconPath);
      } catch (Exception e) {
        getLogger().info("Unable to load your server-icon.png, continuing without it.", e);
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

  public String getMotd() {
    return motd;
  }

  /**
   * Returns the proxy's MOTD.
   *
   * @return the MOTD
   */
  @Override
  public Component getMotdComponent() {
    if (motdAsComponent == null) {
      if (motd.startsWith("{")) {
        motdAsComponent = GsonComponentSerializer.INSTANCE.deserialize(motd);
      } else {
        motdAsComponent = LegacyComponentSerializer.legacy().deserialize(motd, '&');
      }
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
    return forwardingSecret;
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

  public boolean isProxyProtocol() {
    return advanced.isProxyProtocol();
  }

  public boolean useTcpFastOpen() {
    return advanced.tcpFastOpen;
  }

  public Metrics getMetrics() {
    return metrics;
  }

  public PingPassthroughMode getPingPassthrough() {
    return pingPassthrough;
  }

  public boolean isBungeePluginChannelEnabled() {
    return advanced.isBungeePluginMessageChannel();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("configVersion", configVersion)
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
        .toString();
  }

  /**
   * Reads the Velocity configuration from {@code path}.
   * @param path the path to read from
   * @return the deserialized Velocity configuration
   * @throws IOException if we could not read from the {@code path}.
   */
  public static VelocityConfiguration read(Path path) throws IOException {
    Toml toml;
    if (!path.toFile().exists()) {
      getLogger().info("No velocity.toml found, creating one for you...");
      return new VelocityConfiguration(new Servers(), new ForcedHosts(), new Advanced(),
          new Query(), new Metrics());
    } else {
      try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
        toml = new Toml().read(reader);
      }
    }

    Servers servers = new Servers(toml.getTable("servers"));
    ForcedHosts forcedHosts = new ForcedHosts(toml.getTable("forced-hosts"));
    Advanced advanced = new Advanced(toml.getTable("advanced"));
    Query query = new Query(toml.getTable("query"));
    Metrics metrics = new Metrics(toml.getTable("metrics"));
    byte[] forwardingSecret = toml.getString("forwarding-secret", generateRandomString(12))
        .getBytes(StandardCharsets.UTF_8);

    String forwardingModeName = toml.getString("player-info-forwarding-mode", "MODERN")
        .toUpperCase(Locale.US);
    String passThroughName = toml.getString("ping-passthrough", "DISABLED")
        .toUpperCase(Locale.US);

    return new VelocityConfiguration(
        toml.getString("bind", "0.0.0.0:25577"),
        toml.getString("motd", "&3A Velocity Server"),
        toml.getLong("show-max-players", 500L).intValue(),
        toml.getBoolean("online-mode", true),
        toml.getBoolean("announce-forge", false),
        PlayerInfoForwarding.valueOf(forwardingModeName),
        forwardingSecret,
        toml.getBoolean("kick-existing-players", false),
        PingPassthroughMode.valueOf(passThroughName),
        servers,
        forcedHosts,
        advanced,
        query,
        metrics
    );
  }

  private static String generateRandomString(int length) {
    String chars = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
    StringBuilder builder = new StringBuilder();
    Random rnd = new Random();
    for (int i = 0; i < length; i++) {
      builder.append(chars.charAt(rnd.nextInt(chars.length())));
    }
    return builder.toString();
  }

  public boolean isOnlineModeKickExistingPlayers() {
    return onlineModeKickExistingPlayers;
  }

  private static class Servers {

    @IsMap
    @Comment({"Configure your servers here. Each key represents the server's name, and the value",
        "represents the IP address of the server to connect to."})
    private Map<String, String> servers = ImmutableMap.of(
        "lobby", "127.0.0.1:30066",
        "factions", "127.0.0.1:30067",
        "minigames", "127.0.0.1:30068"
    );

    @Comment("In what order we should try servers when a player logs in or is kicked from a"
        + "server.")
    @ConfigKey("try")
    private List<String> attemptConnectionOrder = Arrays.asList("lobby");

    private Servers() {
    }

    private Servers(Toml toml) {
      if (toml != null) {
        Map<String, String> servers = new HashMap<>();
        for (Map.Entry<String, Object> entry : toml.entrySet()) {
          if (entry.getValue() instanceof String) {
            servers.put(cleanServerName(entry.getKey()), (String) entry.getValue());
          } else {
            if (!entry.getKey().equalsIgnoreCase("try")) {
              throw new IllegalArgumentException(
                  "Server entry " + entry.getKey() + " is not a string!");
            }
          }
        }
        this.servers = ImmutableMap.copyOf(servers);
        this.attemptConnectionOrder = toml.getList("try", attemptConnectionOrder);
      }
    }

    private Servers(Map<String, String> servers, List<String> attemptConnectionOrder) {
      this.servers = servers;
      this.attemptConnectionOrder = attemptConnectionOrder;
    }

    private Map<String, String> getServers() {
      return servers;
    }

    public void setServers(Map<String, String> servers) {
      this.servers = servers;
    }

    public List<String> getAttemptConnectionOrder() {
      return attemptConnectionOrder;
    }

    public void setAttemptConnectionOrder(List<String> attemptConnectionOrder) {
      this.attemptConnectionOrder = attemptConnectionOrder;
    }

    /**
     * TOML requires keys to match a regex of {@code [A-Za-z0-9_-]} unless it is wrapped in
     * quotes; however, the TOML parser returns the key with the quotes so we need to clean the
     * server name before we pass it onto server registration to keep proper server name behavior.
     *
     * @param name the server name to clean
     *
     * @return the cleaned server name
     */
    private String cleanServerName(String name) {
      return name.replace("\"", "");
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

    @IsMap
    @Comment("Configure your forced hosts here.")
    private Map<String, List<String>> forcedHosts = ImmutableMap.of(
        "lobby.example.com", ImmutableList.of("lobby"),
        "factions.example.com", ImmutableList.of("factions"),
        "minigames.example.com", ImmutableList.of("minigames")
    );

    private ForcedHosts() {
    }

    private ForcedHosts(Toml toml) {
      if (toml != null) {
        Map<String, List<String>> forcedHosts = new HashMap<>();
        for (Map.Entry<String, Object> entry : toml.entrySet()) {
          if (entry.getValue() instanceof String) {
            forcedHosts.put(unescapeKeyIfNeeded(entry.getKey()), ImmutableList.of(
                (String) entry.getValue()));
          } else if (entry.getValue() instanceof List) {
            forcedHosts.put(unescapeKeyIfNeeded(entry.getKey()),
                ImmutableList.copyOf((List<String>) entry.getValue()));
          } else {
            throw new IllegalStateException(
                "Invalid value of type " + entry.getValue().getClass() + " in forced hosts!");
          }
        }
        this.forcedHosts = ImmutableMap.copyOf(forcedHosts);
      }
    }

    private ForcedHosts(Map<String, List<String>> forcedHosts) {
      this.forcedHosts = forcedHosts;
    }

    private Map<String, List<String>> getForcedHosts() {
      return forcedHosts;
    }

    private void setForcedHosts(Map<String, List<String>> forcedHosts) {
      this.forcedHosts = forcedHosts;
    }

    @Override
    public String toString() {
      return "ForcedHosts{"
          + "forcedHosts=" + forcedHosts
          + '}';
    }
  }

  private static class Advanced {

    @Comment({
        "How large a Minecraft packet has to be before we compress it. Setting this to zero will",
        "compress all packets, and setting it to -1 will disable compression entirely."
    })
    @ConfigKey("compression-threshold")
    private int compressionThreshold = 256;

    @Comment({"How much compression should be done (from 0-9). The default is -1, which uses the",
        "default level of 6."})
    @ConfigKey("compression-level")
    private int compressionLevel = -1;

    @Comment({
        "How fast (in milliseconds) are clients allowed to connect after the last connection? By",
        "default, this is three seconds. Disable this by setting this to 0."
    })
    @ConfigKey("login-ratelimit")
    private int loginRatelimit = 3000;

    @Comment({
        "Specify a custom timeout for connection timeouts here. The default is five seconds."})
    @ConfigKey("connection-timeout")
    private int connectionTimeout = 5000;

    @Comment({"Specify a read timeout for connections here. The default is 30 seconds."})
    @ConfigKey("read-timeout")
    private int readTimeout = 30000;

    @Comment("Enables compatibility with HAProxy.")
    @ConfigKey("proxy-protocol")
    private boolean proxyProtocol = false;

    @Comment("Enables TCP fast open support on the proxy. Requires the proxy to run on Linux.")
    @ConfigKey("tcp-fast-open")
    private boolean tcpFastOpen = false;

    @Comment("Enables BungeeCord plugin messaging channel support on Velocity.")
    @ConfigKey("bungee-plugin-message-channel")
    private boolean bungeePluginMessageChannel = true;

    private Advanced() {
    }

    private Advanced(Toml toml) {
      if (toml != null) {
        this.compressionThreshold = toml.getLong("compression-threshold", 256L).intValue();
        this.compressionLevel = toml.getLong("compression-level", -1L).intValue();
        this.loginRatelimit = toml.getLong("login-ratelimit", 3000L).intValue();
        this.connectionTimeout = toml.getLong("connection-timeout", 5000L).intValue();
        this.readTimeout = toml.getLong("read-timeout", 30000L).intValue();
        this.proxyProtocol = toml.getBoolean("proxy-protocol", false);
        this.tcpFastOpen = toml.getBoolean("tcp-fast-open", false);
        this.bungeePluginMessageChannel = toml.getBoolean("bungee-plugin-message-channel", true);
      }
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

    public boolean isTcpFastOpen() {
      return tcpFastOpen;
    }

    public boolean isBungeePluginMessageChannel() {
      return bungeePluginMessageChannel;
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
          + '}';
    }
  }

  private static class Query {

    @Comment("Whether to enable responding to GameSpy 4 query responses or not.")
    @ConfigKey("enabled")
    private boolean queryEnabled = false;

    @Comment("If query is enabled, on what port should the query protocol listen on?")
    @ConfigKey("port")
    private int queryPort = 25577;

    @Comment("This is the map name that is reported to the query services.")
    @ConfigKey("map")
    private String queryMap = "Velocity";

    @Comment("Whether plugins should be shown in query response by default or not")
    @ConfigKey("show-plugins")
    private boolean showPlugins = false;

    private Query() {
    }

    private Query(boolean queryEnabled, int queryPort, String queryMap, boolean showPlugins) {
      this.queryEnabled = queryEnabled;
      this.queryPort = queryPort;
      this.queryMap = queryMap;
      this.showPlugins = showPlugins;
    }

    private Query(Toml toml) {
      if (toml != null) {
        this.queryEnabled = toml.getBoolean("enabled", false);
        this.queryPort = toml.getLong("port", 25577L).intValue();
        this.queryMap = toml.getString("map", "Velocity");
        this.showPlugins = toml.getBoolean("show-plugins", false);
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

  public static class Metrics {

    @Comment({"Whether metrics will be reported to bStats (https://bstats.org).",
        "bStats collects some basic information, like how many people use Velocity and their",
        "player count. We recommend keeping bStats enabled, but if you're not comfortable with",
        "this, you can turn this setting off. There is no performance penalty associated with",
        "having metrics enabled, and data sent to bStats can't identify your server."})
    @ConfigKey("enabled")
    private boolean enabled = true;

    @Comment("A unique, anonymous ID to identify this proxy with.")
    @ConfigKey("id")
    private String id = UUID.randomUUID().toString();

    @ConfigKey("log-failure")
    private boolean logFailure = false;

    @Ignore
    private boolean fromConfig;

    private Metrics() {
      this.fromConfig = false;
    }

    private Metrics(Toml toml) {
      if (toml != null) {
        this.enabled = toml.getBoolean("enabled", false);
        this.id = toml.getString("id", UUID.randomUUID().toString());
        this.logFailure = toml.getBoolean("log-failure", false);
        this.fromConfig = true;
      }
    }

    public boolean isEnabled() {
      return enabled;
    }

    public String getId() {
      return id;
    }

    public boolean isLogFailure() {
      return logFailure;
    }

    public boolean isFromConfig() {
      return fromConfig;
    }
  }
}
