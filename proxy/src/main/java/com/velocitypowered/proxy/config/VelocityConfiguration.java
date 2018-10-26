package com.velocitypowered.proxy.config;

import com.google.common.collect.ImmutableMap;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.util.AddressUtil;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class VelocityConfiguration extends AnnotatedConfig implements ProxyConfig {

    @Comment("Config version. Do not change this")
    @ConfigKey("config-version")
    private final String configVersion = "1.0";

    @Comment("What port should the proxy be bound to? By default, we'll bind to all addresses on port 25577.")
    private String bind = "0.0.0.0:25577";

    @Comment("What should be the MOTD? Legacy color codes and JSON are accepted.")
    private String motd = "&3A Velocity Server";

    @Comment({
        "What should we display for the maximum number of players? (Velocity does not support a cap",
        "on the number of players online.)"})
    @ConfigKey("show-max-players")
    private int showMaxPlayers = 500;

    @Comment("Should we authenticate players with Mojang? By default, this is on.")
    @ConfigKey("online-mode")
    private boolean onlineMode = true;

    @Comment({
        "Should we forward IP addresses and other data to backend servers?",
        "Available options:",
        "- \"none\":   No forwarding will be done. All players will appear to be connecting from the proxy",
        "            and will have offline-mode UUIDs.",
        "- \"legacy\": Forward player IPs and UUIDs in BungeeCord-compatible fashion. Use this if you run",
        "            servers using Minecraft 1.12 or lower.",
        "- \"modern\": Forward player IPs and UUIDs as part of the login process using Velocity's native",
        "            forwarding. Only applicable for Minecraft 1.13 or higher."})
    @ConfigKey("player-info-forwarding-mode")
    private PlayerInfoForwarding playerInfoForwardingMode = PlayerInfoForwarding.NONE;

    @StringAsBytes
    @Comment("If you are using modern IP forwarding, configure an unique secret here.")
    @ConfigKey("forwarding-secret")
    private byte[] forwardingSecret = generateRandomString(12).getBytes(StandardCharsets.UTF_8);

    @Comment("Announce whether or not your server supports Forge/FML. If you run a modded server, we suggest turning this on.")
    @ConfigKey("announce-forge")
    private boolean announceForge = false;

    @Table("[servers]")
    private final Servers servers;

    @Table("[advanced]")
    private final Advanced advanced;

    @Table("[query]")
    private final Query query;

    @Ignore
    private Component motdAsComponent;
    @Ignore
    private Favicon favicon;

    public VelocityConfiguration(Servers servers, Advanced advanced, Query query) {
        this.servers = servers;
        this.advanced = advanced;
        this.query = query;
    }

    private VelocityConfiguration(String bind, String motd, int showMaxPlayers, boolean onlineMode,
            boolean announceForge, PlayerInfoForwarding playerInfoForwardingMode, byte[] forwardingSecret,
            Servers servers, Advanced advanced, Query query) {
        this.bind = bind;
        this.motd = motd;
        this.showMaxPlayers = showMaxPlayers;
        this.onlineMode = onlineMode;
        this.announceForge = announceForge;
        this.playerInfoForwardingMode = playerInfoForwardingMode;
        this.forwardingSecret = forwardingSecret;
        this.servers = servers;
        this.advanced = advanced;
        this.query = query;
    }

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
                logger.warn("Player info forwarding is disabled! All players will appear to be connecting from the proxy and will have offline-mode UUIDs.");
                break;
            case MODERN:
                if (forwardingSecret == null || forwardingSecret.length == 0) {
                    logger.error("You don't have a forwarding secret set. This is required for security.");
                    valid = false;
                }
                break;
        }

        if (servers.getServers().isEmpty()) {
            logger.error("You have no servers configured. :(");
            valid = false;
        } else {
            if (servers.getAttemptConnectionOrder().isEmpty()) {
                logger.error("No fallback servers are configured!");
                valid = false;
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
            logger.warn("ALL packets going through the proxy will be uncompressed. This will increase bandwidth usage.");
        }

        if (advanced.compressionThreshold < -1) {
            logger.error("Invalid compression threshold {}", advanced.compressionLevel);
            valid = false;
        } else if (advanced.compressionThreshold == 0) {
            logger.warn("ALL packets going through the proxy will be compressed. This will compromise throughput and increase CPU usage!");
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
        return AddressUtil.parseAddress(bind);
    }

    public boolean isQueryEnabled() {
        return query.isQueryEnabled();
    }

    public int getQueryPort() {
        return query.getQueryPort();
    }

    public String getQueryMap() {
        return query.getQueryMap();
    }

    public String getMotd() {
        return motd;
    }

    public Component getMotdComponent() {
        if (motdAsComponent == null) {
            if (motd.startsWith("{")) {
                motdAsComponent = ComponentSerializers.JSON.deserialize(motd);
            } else {
                motdAsComponent = ComponentSerializers.LEGACY.deserialize(motd, '&');
            }
        }
        return motdAsComponent;
    }

    public int getShowMaxPlayers() {
        return showMaxPlayers;
    }

    public boolean isOnlineMode() {
        return onlineMode;
    }

    public PlayerInfoForwarding getPlayerInfoForwardingMode() {
        return playerInfoForwardingMode;
    }

    public byte[] getForwardingSecret() {
        return forwardingSecret;
    }

    public Map<String, String> getServers() {
        return servers.getServers();
    }

    public List<String> getAttemptConnectionOrder() {
        return servers.getAttemptConnectionOrder();
    }

    public int getCompressionThreshold() {
        return advanced.getCompressionThreshold();
    }

    public int getCompressionLevel() {
        return advanced.getCompressionLevel();
    }

    public int getLoginRatelimit() {
        return advanced.getLoginRatelimit();
    }

    public Optional<Favicon> getFavicon() {
        return Optional.ofNullable(favicon);
    }

    public boolean isAnnounceForge() {
        return announceForge;
    }

    public int getConnectTimeout() {
        return advanced.getConnectionTimeout();
    }

    public int getReadTimeout() {
        return advanced.getReadTimeout();
    }

    public boolean isProxyProtocol() {
        return advanced.isProxyProtocol();
    }

    public static VelocityConfiguration read(Path path) throws IOException {
        Toml toml;
        if (!path.toFile().exists()) {
            getLogger().info("No velocity.toml found, creating one for you...");
            return new VelocityConfiguration(new Servers(), new Advanced(), new Query());
        } else {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                toml = new Toml().read(reader);
            }
        }

        Servers servers = new Servers(toml.getTable("servers"));
        Advanced advanced = new Advanced(toml.getTable("advanced"));
        Query query = new Query(toml.getTable("query"));
        byte[] forwardingSecret = toml.getString("forwarding-secret", "5up3r53cr3t")
                .getBytes(StandardCharsets.UTF_8);

        VelocityConfiguration configuration = new VelocityConfiguration(
                toml.getString("bind", "0.0.0.0:25577"),
                toml.getString("motd", "&3A Velocity Server"),
                toml.getLong("show-max-players", 500L).intValue(),
                toml.getBoolean("online-mode", true),
                toml.getBoolean("announce-forge", false),
                PlayerInfoForwarding.valueOf(toml.getString("player-info-forwarding-mode", "MODERN").toUpperCase()),
                forwardingSecret,
                servers,
                advanced,
                query
        );
        upgradeConfig(configuration, toml);
        return configuration;
    }

    private static void upgradeConfig(VelocityConfiguration configuration, Toml toml) {
        switch (toml.getString("config-version", configuration.configVersion)) {
            case "1.0":
                //TODO: Upgrade a 1.0 config to a new version. Maybe add a recursive support in future.
                break;
            default:
                break;
        }
    }

    private static String generateRandomString(int lenght) {
        String chars = "AaBbCcDdEeFfGgHhIiJjKkLlMmNnOoPpQqRrSsTtUuVvWwXxYyZz1234567890";
        StringBuilder builder = new StringBuilder();
        Random rnd = new Random();
        for (int i = 0; i < lenght; i++) {
            builder.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return builder.toString();
    }

    private static class Servers {

        @IsMap
        @Comment("Configure your servers here.")
        private Map<String, String> servers = ImmutableMap.of("lobby", "127.0.0.1:30066", "factions", "127.0.0.1:30067", "minigames", "127.0.0.1:30068");

        @Comment("In what order we should try servers when a player logs in or is kicked from a server.")
        @ConfigKey("try")
        private List<String> attemptConnectionOrder = Arrays.asList("lobby");

        private Servers() {
        }

        private Servers(Toml toml) {
            if (toml != null) {
                Map<String, String> servers = new HashMap<>();
                for (Map.Entry<String, Object> entry : toml.entrySet()) {
                    if (entry.getValue() instanceof String) {
                        servers.put(entry.getKey(), (String) entry.getValue());
                    } else {
                        if (!entry.getKey().equalsIgnoreCase("try")) {
                            throw new IllegalArgumentException("Server entry " + entry.getKey() + " is not a string!");
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
    }

    private static class Advanced {

        @Comment({
            "How large a Minecraft packet has to be before we compress it. Setting this to zero will compress all packets, and",
            "setting it to -1 will disable compression entirely."})
        @ConfigKey("compression-threshold")
        private int compressionThreshold = 1024;
        @Comment("How much compression should be done (from 0-9). The default is -1, which uses zlib's default level of 6.")
        @ConfigKey("compression-level")
        private int compressionLevel = -1;
        @Comment({
            "How fast (in miliseconds) are clients allowed to connect after the last connection? Default: 3000",
            "Disable by setting to 0"})
        @ConfigKey("login-ratelimit")
        private int loginRatelimit = 3000;
        @Comment({"Specify a custom timeout for connection timeouts here. The default is five seconds."})
        @ConfigKey("connection-timeout")
        private int connectionTimeout = 5000;
        @Comment({"Specify a read timeout for connections here. The default is 30 seconds."})
        @ConfigKey("read-timeout")
        private int readTimeout = 30000;
        @Comment("Enables compatibility with HAProxy.")
        @ConfigKey("proxy-protocol")
        private boolean proxyProtocol = false;

        private Advanced() {
        }

        private Advanced(Toml toml) {
            if (toml != null) {
                this.compressionThreshold = toml.getLong("compression-threshold", 1024L).intValue();
                this.compressionLevel = toml.getLong("compression-level", -1L).intValue();
                this.loginRatelimit = toml.getLong("login-ratelimit", 3000L).intValue();
                this.connectionTimeout = toml.getLong("connection-timeout", 5000L).intValue();
                this.readTimeout = toml.getLong("read-timeout", 30000L).intValue();
                this.proxyProtocol = toml.getBoolean("proxy-protocol", false);
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
    }

    private static class Query {

        @Comment("Whether to enable responding to GameSpy 4 query responses or not")
        @ConfigKey("enabled")
        private boolean queryEnabled = false;
        @Comment("If query responding is enabled, on what port should query response listener listen on?")
        @ConfigKey("port")
        private int queryPort = 25577;
        @Comment("This is the map name that is reported to the query services.")
        @ConfigKey("map")
        private String queryMap = "Velocity";

        private Query() {
        }

        private Query(boolean queryEnabled, int queryPort) {
            this.queryEnabled = queryEnabled;
            this.queryPort = queryPort;
            this.queryMap = queryMap;
        }

        private Query(Toml toml) {
            if (toml != null) {
                this.queryEnabled = toml.getBoolean("enabled", false);
                this.queryPort = toml.getLong("port", 25577L).intValue();
                this.queryMap = toml.getString("map", "Velocity");
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
    }
}
