package com.velocitypowered.proxy.config;

import com.google.common.collect.ImmutableMap;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.server.Favicon;
import com.velocitypowered.proxy.util.AddressUtil;
import com.velocitypowered.api.util.LegacyChatColorUtils;
import io.netty.buffer.ByteBufUtil;
import net.kyori.text.Component;
import net.kyori.text.serializer.ComponentSerializers;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

public class VelocityConfiguration {
    private static final Logger logger = LogManager.getLogger(VelocityConfiguration.class);

    private final String bind;
    private final String motd;
    private final int showMaxPlayers;
    private final boolean onlineMode;
    private final PlayerInfoForwarding playerInfoForwardingMode;
    private final Map<String, String> servers;
    private final List<String> attemptConnectionOrder;
    private final int compressionThreshold;
    private final int compressionLevel;
    private final int loginRatelimit;

    private final boolean queryEnabled;
    private final int queryPort;

    private Component motdAsComponent;
    private Favicon favicon;

    private final byte[] forwardingSecret;

    private VelocityConfiguration(String bind, String motd, int showMaxPlayers, boolean onlineMode,
                                  PlayerInfoForwarding playerInfoForwardingMode, Map<String, String> servers,
                                  List<String> attemptConnectionOrder, int compressionThreshold,
                                  int compressionLevel, int loginRatelimit, boolean queryEnabled,
                                  int queryPort, byte[] forwardingSecret) {
        this.bind = bind;
        this.motd = motd;
        this.showMaxPlayers = showMaxPlayers;
        this.onlineMode = onlineMode;
        this.playerInfoForwardingMode = playerInfoForwardingMode;
        this.servers = servers;
        this.attemptConnectionOrder = attemptConnectionOrder;
        this.compressionThreshold = compressionThreshold;
        this.compressionLevel = compressionLevel;
        this.loginRatelimit = loginRatelimit;
        this.queryEnabled = queryEnabled;
        this.queryPort = queryPort;
        this.forwardingSecret = forwardingSecret;
    }

    public boolean validate() {
        boolean valid = true;

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

        if (servers.isEmpty()) {
            logger.error("You have no servers configured. :(");
            valid = false;
        } else {
            if (attemptConnectionOrder.isEmpty()) {
                logger.error("No fallback servers are configured!");
                valid = false;
            }

            for (Map.Entry<String, String> entry : servers.entrySet()) {
                try {
                    AddressUtil.parseAddress(entry.getValue());
                } catch (IllegalArgumentException e) {
                    logger.error("Server {} does not have a valid IP address.", entry.getKey(), e);
                    valid = false;
                }
            }

            for (String s : attemptConnectionOrder) {
                if (!servers.containsKey(s)) {
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

        if (compressionLevel < -1 || compressionLevel > 9) {
            logger.error("Invalid compression level {}", compressionLevel);
            valid = false;
        } else if (compressionLevel == 0) {
            logger.warn("ALL packets going through the proxy are going to be uncompressed. This will increase bandwidth usage.");
        }

        if (compressionThreshold < -1) {
            logger.error("Invalid compression threshold {}", compressionLevel);
            valid = false;
        } else if (compressionThreshold == 0) {
            logger.warn("ALL packets going through the proxy are going to be compressed. This may hurt performance.");
        }

        if (loginRatelimit < 0) {
            logger.error("Invalid login ratelimit {}", loginRatelimit);
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
                logger.info("Unable to load your server-icon.png, continuing without it.", e);
            }
        }
    }

    public InetSocketAddress getBind() {
        return AddressUtil.parseAddress(bind);
    }

    public boolean isQueryEnabled() {
        return queryEnabled;
    }

    public int getQueryPort() {
        return queryPort;
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
        return servers;
    }

    public List<String> getAttemptConnectionOrder() {
        return attemptConnectionOrder;
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

    public Favicon getFavicon() {
        return favicon;
    }

    public byte[] getForwardingSecret() {
        return forwardingSecret;
    }

    @Override
    public String toString() {
        return "VelocityConfiguration{" +
                "bind='" + bind + '\'' +
                ", motd='" + motd + '\'' +
                ", showMaxPlayers=" + showMaxPlayers +
                ", onlineMode=" + onlineMode +
                ", playerInfoForwardingMode=" + playerInfoForwardingMode +
                ", servers=" + servers +
                ", attemptConnectionOrder=" + attemptConnectionOrder +
                ", compressionThreshold=" + compressionThreshold +
                ", compressionLevel=" + compressionLevel +
                ", loginRatelimit=" + loginRatelimit +
                ", queryEnabled=" + queryEnabled +
                ", queryPort=" + queryPort +
                ", motdAsComponent=" + motdAsComponent +
                ", favicon=" + favicon +
                ", forwardingSecret=" + ByteBufUtil.hexDump(forwardingSecret) +
                '}';
    }

    public static VelocityConfiguration read(Path path) throws IOException {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Toml toml = new Toml().read(reader);

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

            byte[] forwardingSecret = toml.getString("player-info-forwarding-secret", "5up3r53cr3t")
                    .getBytes(StandardCharsets.UTF_8);

            return new VelocityConfiguration(
                    toml.getString("bind", "0.0.0.0:25577"),
                    toml.getString("motd", "&3A Velocity Server"),
                    toml.getLong("show-max-players", 500L).intValue(),
                    toml.getBoolean("online-mode", true),
                    PlayerInfoForwarding.valueOf(toml.getString("player-info-forwarding", "MODERN").toUpperCase()),
                    ImmutableMap.copyOf(servers),
                    toml.getTable("servers").getList("try"),
                    toml.getTable("advanced").getLong("compression-threshold", 1024L).intValue(),
                    toml.getTable("advanced").getLong("compression-level", -1L).intValue(),
                    toml.getTable("advanced").getLong("login-ratelimit", 3000L).intValue(),
                    toml.getTable("query").getBoolean("enabled", false),
                    toml.getTable("query").getLong("port", 25577L).intValue(),
                    forwardingSecret);
        }
    }
}
