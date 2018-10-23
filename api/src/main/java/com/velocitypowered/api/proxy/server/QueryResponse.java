package com.velocitypowered.api.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * GS4 query response. This class is immutable.
 */
public final class QueryResponse {
    private final String hostname;
    private final String gameVersion;
    private final String map;
    private final int currentPlayers;
    private final int maxPlayers;
    private final String proxyHost;
    private final int proxyPort;
    private final Collection<String> players;
    private final String proxyVersion;
    private final Collection<PluginInformation> plugins;

    private QueryResponse(String hostname, String gameVersion, String map, int currentPlayers, int maxPlayers, String proxyHost, int proxyPort, Collection<String> players, String proxyVersion, Collection<PluginInformation> plugins) {
        this.hostname = hostname;
        this.gameVersion = gameVersion;
        this.map = map;
        this.currentPlayers = currentPlayers;
        this.maxPlayers = maxPlayers;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.players = players;
        this.proxyVersion = proxyVersion;
        this.plugins = plugins;
    }

    /**
     * Get hostname which will be used to reply to the query. By default it is {@link ProxyConfig#getMotdComponent()} in plain text without colour codes.
     * @return hostname
     */
    @NonNull
    public String getHostname() {
        return hostname;
    }

    /**
     * Get game version which will be used to reply to the query. By default supported Minecraft versions range is sent.
     * @return game version
     */
    @NonNull
    public String getGameVersion() {
        return gameVersion;
    }

    /**
     * Get map name which will be used to reply to the query. By default {@link ProxyConfig#getQueryMap()} is sent.
     * @return map name
     */
    @NonNull
    public String getMap() {
        return map;
    }

    /**
     * Get current online player count which will be used to reply to the query.
     * @return online player count
     */
    public int getCurrentPlayers() {
        return currentPlayers;
    }

    /**
     * Get max player count which will be used to reply to the query.
     * @return max player count
     */
    public int getMaxPlayers() {
        return maxPlayers;
    }

    /**
     * Get proxy (public facing) hostname
     * @return proxy hostname
     */
    @NonNull
    public String getProxyHost() {
        return proxyHost;
    }

    /**
     * Get proxy (public facing) port
     * @return proxy port
     */
    public int getProxyPort() {
        return proxyPort;
    }

    /**
     * Get collection of players which will be used to reply to the query.
     * @return collection of players
     */
    @NonNull
    public Collection<String> getPlayers() {
        return players;
    }

    /**
     * Get server software (name and version) which will be used to reply to the query.
     * @return server software
     */
    @NonNull
    public String getProxyVersion() {
        return proxyVersion;
    }

    /**
     * Get list of plugins which will be used to reply to the query.
     * @return collection of plugins
     */
    @NonNull
    public Collection<PluginInformation> getPlugins() {
        return plugins;
    }


    /**
     * Creates a new {@link Builder} instance from data represented by this response
     * @return {@link QueryResponse} builder
     */
    @NonNull
    public Builder toBuilder() {
        return QueryResponse.builder()
                .hostname(getHostname())
                .gameVersion(getGameVersion())
                .map(getMap())
                .currentPlayers(getCurrentPlayers())
                .maxPlayers(getMaxPlayers())
                .proxyHost(getProxyHost())
                .proxyPort(getProxyPort())
                .players(getPlayers())
                .proxyVersion(getProxyVersion())
                .plugins(getPlugins());
    }

    /**
     * Creates a new {@link Builder} instance
     * @return {@link QueryResponse} builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * A builder for {@link QueryResponse} objects.
     */
    public static final class Builder {
        private String hostname;
        private String gameVersion;
        private String map;
        private int currentPlayers;
        private int maxPlayers;
        private String proxyHost;
        private int proxyPort;
        private Collection<String> players = new ArrayList<>();
        private String proxyVersion;
        private List<PluginInformation> plugins = new ArrayList<>();

        private Builder() {}

        @NonNull
        public Builder hostname(@NonNull String hostname) {
            this.hostname = Preconditions.checkNotNull(hostname, "hostname");
            return this;
        }

        @NonNull
        public Builder gameVersion(@NonNull String gameVersion) {
            this.gameVersion = Preconditions.checkNotNull(gameVersion, "gameVersion");
            return this;
        }

        @NonNull
        public Builder map(@NonNull String map) {
            this.map = Preconditions.checkNotNull(map, "map");
            return this;
        }

        @NonNull
        public Builder currentPlayers(int currentPlayers) {
            Preconditions.checkArgument(currentPlayers >= 0, "currentPlayers cannot be negative");
            this.currentPlayers = currentPlayers;
            return this;
        }

        @NonNull
        public Builder maxPlayers(int maxPlayers) {
            Preconditions.checkArgument(maxPlayers >= 0, "maxPlayers cannot be negative");
            this.maxPlayers = maxPlayers;
            return this;
        }

        @NonNull
        public Builder proxyHost(@NonNull String proxyHost) {
            this.proxyHost = Preconditions.checkNotNull(proxyHost, "proxyHost");
            return this;
        }

        @NonNull
        public Builder proxyPort(int proxyPort) {
            Preconditions.checkArgument(proxyPort >= 1 && proxyPort <= 65535, "proxyPort must be between 1-65535");
            this.proxyPort = proxyPort;
            return this;
        }

        @NonNull
        public Builder players(@NonNull Collection<String> players) {
            this.players.addAll(Preconditions.checkNotNull(players, "players"));
            return this;
        }

        @NonNull
        public Builder players(@NonNull String... players) {
            this.players.addAll(Arrays.asList(Preconditions.checkNotNull(players, "players")));
            return this;
        }

        @NonNull
        public Builder clearPlayers() {
            this.players.clear();
            return this;
        }

        @NonNull
        public Builder proxyVersion(@NonNull String proxyVersion) {
            this.proxyVersion = Preconditions.checkNotNull(proxyVersion, "proxyVersion");
            return this;
        }

        @NonNull
        public Builder plugins(@NonNull Collection<PluginInformation> plugins) {
            this.plugins.addAll(Preconditions.checkNotNull(plugins, "plugins"));
            return this;
        }

        @NonNull
        public Builder plugins(@NonNull PluginInformation... plugins) {
            this.plugins.addAll(Arrays.asList(Preconditions.checkNotNull(plugins, "plugins")));
            return this;
        }

        @NonNull
        public Builder clearPlugins() {
            this.plugins.clear();
            return this;
        }

        /**
         * Builds new {@link QueryResponse} with supplied data
         * @return response
         */
        @NonNull
        public QueryResponse build() {
            return new QueryResponse(
                    hostname,
                    gameVersion,
                    map,
                    currentPlayers,
                    maxPlayers,
                    proxyHost,
                    proxyPort,
                    ImmutableList.copyOf(players),
                    proxyVersion,
                    ImmutableList.copyOf(plugins)
            );
        }
    }

    /**
     * Plugin information
     */
    public static class PluginInformation {
        private String name;
        private String version;

        public PluginInformation(@NonNull String name, @Nullable String version) {
            this.name = name;
            this.version = version;
        }

        @NonNull
        public String getName() {
            return name;
        }

        public void setName(@NonNull String name) {
            this.name = name;
        }

        public void setVersion(@Nullable String version) {
            this.version = version;
        }

        @Nullable
        public String getVersion() {
            return version;
        }

        @NonNull
        public static PluginInformation of(@NonNull String name, @Nullable String version) {
            return new PluginInformation(name, version);
        }
    }
}
