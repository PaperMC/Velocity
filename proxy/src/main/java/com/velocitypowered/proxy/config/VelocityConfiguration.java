package com.velocitypowered.proxy.config;

import com.google.common.collect.ImmutableMap;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.api.util.LegacyChatColorUtils;
import com.velocitypowered.proxy.VelocityServer;
import io.netty.buffer.ByteBufUtil;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;

import java.io.IOException;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Logger;

public class VelocityConfiguration extends AnnotationConfig {

    @Comment("What port should the proxy be bound to? By default, we'll bind to all addresses on port 25577.")
    private final String bind;
    @Comment("What should be the MOTD? Legacy color codes and JSON are accepted.")
    private final String motd;
    @Comment({"What should we display for the maximum number of players? (Velocity does not support a cap",
        "on the number of players online.)"})
    @CfgKey("show-max-players")
    private final int showMaxPlayers;
    @Comment("Should we authenticate players with Mojang? By default, this is on.")
    @CfgKey("online-mode")
    private final boolean onlineMode;
    @Comment({"Should we forward IP addresses and other data to backend servers?",
        "Available options:",
        "- \"none\":   No forwarding will be done. All players will appear to be Should we forward IP addresses and other data to backend servers?connecting from the proxy",
        "            and will have offline-mode UUIDs.",
        "- \"legacy\": Forward player IPs and UUIDs in BungeeCord-compatible fashion. Use this if you run",
        "            servers using Minecraft 1.12 or lower.",
        "- \"modern\": Forward player IPs and UUIDs as part of the login process using Velocity's native",
        "            forwarding. Only applicable for Minecraft 1.13 or higher."})
    @CfgKey("player-info-forwarding-mode")
    private final PlayerInfoForwarding playerInfoForwardingMode;

    @AsBytes
    @Comment("If you are using modern IP forwarding, configure an unique secret here.")
    @CfgKey("forwarding-secret")
    private final byte[] forwardingSecret;

    @Table("[servers]")
    private final Servers servers;

    private static class Servers {

        @AsMap
        @Comment("Configure your servers here.")
        public final Map<String, String> servers;

        @Comment("In what order we should try servers when a player logs in or is kicked from a server.")
        @CfgKey("try")
        public final List<String> attemptConnectionOrder;

        public Servers(Map<String, String> servers, List<String> attemptConnectionOrder) {
            this.servers = servers;
            this.attemptConnectionOrder = attemptConnectionOrder;
        }

        @Override
        public String toString() {
            return "Servers{" + "servers=" + servers + ", attemptConnectionOrder=" + attemptConnectionOrder + '}';
        }

    }

    @Table("[advanced]")
    private final Advanced advanced;

    private static class Advanced {

        @Comment({"How large a Minecraft packet has to be before we compress it. Setting this to zero will compress all packets, and",
            "setting it to -1 will disable compression entirely."})
        @CfgKey("compression-threshold")
        public final int compressionThreshold;
        @Comment("How much compression should be done (from 0-9). The default is -1, which uses zlib's default level of 6.")
        @CfgKey("compression-level")
        public final int compressionLevel;
        @Comment({"How fast (in miliseconds) are clients allowed to connect after the last connection? Default: 3000",
            "Disable by setting to 0"})
        @CfgKey("login-ratelimit")
        public final int loginRatelimit;

        public Advanced(Toml toml) {
            this.compressionThreshold = toml.getLong("compression-threshold", 1024L).intValue();
            this.compressionLevel = toml.getLong("compression-level", -1L).intValue();
            this.loginRatelimit = toml.getLong("login-ratelimit", 3000L).intValue();
        }

        @Override
        public String toString() {
            return "Advanced{" + "compressionThreshold=" + compressionThreshold + ", compressionLevel=" + compressionLevel + ", loginRatelimit=" + loginRatelimit + '}';
        }
    }

    @Table("[query]")
    private final Query query;

    private static class Query {

        @Comment("Whether to enable responding to GameSpy 4 query responses or not")
        @CfgKey("enabled")
        public final boolean queryEnabled;
        @Comment("If query responding is enabled, on what port should query response listener listen on?")
        @CfgKey("port")
        public final int queryPort;

        public Query(boolean queryEnabled, int queryPort) {
            this.queryEnabled = queryEnabled;
            this.queryPort = queryPort;
        }

        private Query(Toml toml) {
            this.queryEnabled = toml.getBoolean("enabled", false);
            this.queryPort = toml.getLong("port", 25577L).intValue();
        }

        @Override
        public String toString() {
            return "Query{" + "queryEnabled=" + queryEnabled + ", queryPort=" + queryPort + '}';
        }
    }
    @Ignore
    private Component motdAsComponent;
    @Ignore
    private Favicon favicon;

    private VelocityConfiguration(String bind, String motd, int showMaxPlayers, boolean onlineMode,
            PlayerInfoForwarding playerInfoForwardingMode, byte[] forwardingSecret, Servers servers,
            Advanced advanced, Query query) {
        this.bind = bind;
        this.motd = motd;
        this.showMaxPlayers = showMaxPlayers;
        this.onlineMode = onlineMode;
        this.playerInfoForwardingMode = playerInfoForwardingMode;
        this.forwardingSecret = forwardingSecret;
        this.servers = servers;
        this.advanced = advanced;
        this.query = query;
    }

    public boolean validate() {
        boolean valid = true;
        Logger logger = AnnotationConfig.getLogger();

        if (bind.isEmpty()) {
            logger.error("'bind' option is empty.");
            valid = false;
        }

        try {
            AddressUtil.parseAddress(bind);
        } catch (IllegalArgumentException e) {
            logger.error("'bind' option does not specify a valid IP address.", e);
            valid = false;
        }

        if (!onlineMode) {
            logger.info("Proxy is running in offline mode!");
        }

        switch (playerInfoForwardingMode) {
            case NONE:
                logger.info("Player info forwarding is disabled! All players will appear to be connecting from the proxy and will have offline-mode UUIDs.");
                break;
            case MODERN:
                if (forwardingSecret.length == 0) {
                    logger.error("You don't have a forwarding secret set.");
                    valid = false;
                }
                break;
        }

        if (servers.servers.isEmpty()) {
            logger.error("You have no servers configured. :(");
            valid = false;
        } else {
            if (servers.attemptConnectionOrder.isEmpty()) {
                logger.error("No fallback servers are configured!");
                valid = false;
            }

            for (Map.Entry<String, String> entry : servers.servers.entrySet()) {
                try {
                    AddressUtil.parseAddress(entry.getValue());
                } catch (IllegalArgumentException e) {
                    logger.error("Server {} does not have a valid IP address.", entry.getKey(), e);
                    valid = false;
                }
            }

            for (String s : servers.attemptConnectionOrder) {
                if (!servers.servers.containsKey(s)) {
                    logger.error("Fallback server " + s + " doesn't exist!");
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
            logger.warn("ALL packets going through the proxy are going to be uncompressed. This will increase bandwidth usage.");
        }

        if (advanced.compressionThreshold < -1) {
            logger.error("Invalid compression threshold {}", advanced.compressionLevel);
            valid = false;
        } else if (advanced.compressionThreshold == 0) {
            logger.warn("ALL packets going through the proxy are going to be compressed. This may hurt performance.");
        }

        if (advanced.loginRatelimit < 0) {
            logger.error("Invalid login ratelimit {}", advanced.loginRatelimit);
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
        return query.queryEnabled;
    }

    public int getQueryPort() {
        return query.queryPort;
    }

    public String getMotd() {
        return motd;
    }

    public Component getMotdComponent() {
        if (motdAsComponent == null) {
            if (motd.startsWith("{")) {
                motdAsComponent = ComponentSerializers.JSON.deserialize(motd);
            } else {
                motdAsComponent = ComponentSerializers.LEGACY.deserialize(LegacyChatColorUtils.translate('&', motd));
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

    public Map<String, String> getServers() {
        return servers.servers;
    }

    public List<String> getAttemptConnectionOrder() {
        return servers.attemptConnectionOrder;
    }

    public int getCompressionThreshold() {
        return advanced.compressionThreshold;
    }

    public int getCompressionLevel() {
        return advanced.compressionLevel;
    }

    public int getLoginRatelimit() {
        return advanced.loginRatelimit;
    }

    public Favicon getFavicon() {
        return favicon;
    }

    public byte[] getForwardingSecret() {
        return forwardingSecret;
    }

    @Override
    public String toString() {

        return "VelocityConfiguration{"
                + "bind='" + bind + '\''
                + ", motd='" + motd + '\''
                + ", showMaxPlayers=" + showMaxPlayers
                + ", onlineMode=" + onlineMode
                + ", playerInfoForwardingMode=" + playerInfoForwardingMode
                + ", servers=" + servers
                + ", advanced=" + advanced
                + ", query=" + query
                + ", motdAsComponent=" + motdAsComponent
                + ", favicon=" + favicon
                + ", forwardingSecret=" + ByteBufUtil.hexDump(forwardingSecret)
                + '}';
    }

    public static VelocityConfiguration read(Path path) throws IOException {
        Toml def = new Toml().read(VelocityServer.class.getResourceAsStream("/velocity.toml"));
        Toml toml;
        if (!path.toFile().exists()) {
            toml = def;
        } else {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                toml = new Toml(def).read(reader);
            }
        }

        // TODO: Upgrdate old values to new, when config will be changed in future
        Map<String, String> servers = new HashMap<>();
        for (Map.Entry<String, Object> entry : toml.getTable("servers").entrySet()) {
            if (entry.getValue() instanceof String) {
                servers.put(entry.getKey(), (String) entry.getValue());
            } else {
                if (!entry.getKey().equalsIgnoreCase("try")) {
                    throw new IllegalArgumentException("Server entry " + entry.getKey() + " is not a string!");
                }
            }
        }

        Servers serversTables = new Servers(ImmutableMap.copyOf(servers), toml.getTable("servers").getList("try"));
        Advanced advanced = new Advanced(toml.getTable("advanced"));
        Query query = new Query(toml.getTable("query"));
        byte[] forwardingSecret = toml.getString("player-info-forwarding-secret", "5up3r53cr3t")
                .getBytes(StandardCharsets.UTF_8);

        return new VelocityConfiguration(
                toml.getString("bind", "0.0.0.0:25577"),
                toml.getString("motd", "&3A Velocity Server"),
                toml.getLong("show-max-players", 500L).intValue(),
                toml.getBoolean("online-mode", true),
                PlayerInfoForwarding.valueOf(toml.getString("player-info-forwarding", "MODERN").toUpperCase()),
                forwardingSecret,
                serversTables,
                advanced,
                query
        );
    }

}
