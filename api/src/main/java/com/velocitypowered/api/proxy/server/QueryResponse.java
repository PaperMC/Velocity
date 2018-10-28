package com.velocitypowered.api.proxy.server;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.proxy.config.ProxyConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

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

  private QueryResponse(String hostname, String gameVersion, String map, int currentPlayers,
      int maxPlayers, String proxyHost, int proxyPort, Collection<String> players,
      String proxyVersion, Collection<PluginInformation> plugins) {
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
   * Get hostname which will be used to reply to the query. By default it is {@link
   * ProxyConfig#getMotdComponent()} in plain text without colour codes.
   *
   * @return hostname
   */
  public String getHostname() {
    return hostname;
  }

  /**
   * Get game version which will be used to reply to the query. By default supported Minecraft
   * versions range is sent.
   *
   * @return game version
   */
  public String getGameVersion() {
    return gameVersion;
  }

  /**
   * Get map name which will be used to reply to the query. By default {@link
   * ProxyConfig#getQueryMap()} is sent.
   *
   * @return map name
   */
  public String getMap() {
    return map;
  }

  /**
   * Get current online player count which will be used to reply to the query.
   *
   * @return online player count
   */
  public int getCurrentPlayers() {
    return currentPlayers;
  }

  /**
   * Get max player count which will be used to reply to the query.
   *
   * @return max player count
   */
  public int getMaxPlayers() {
    return maxPlayers;
  }

  /**
   * Get proxy (public facing) hostname.
   *
   * @return proxy hostname
   */
  public String getProxyHost() {
    return proxyHost;
  }

  /**
   * Get proxy (public facing) port.
   *
   * @return proxy port
   */
  public int getProxyPort() {
    return proxyPort;
  }

  /**
   * Get collection of players which will be used to reply to the query.
   *
   * @return collection of players
   */
  public Collection<String> getPlayers() {
    return players;
  }

  /**
   * Get server software (name and version) which will be used to reply to the query.
   *
   * @return server software
   */
  public String getProxyVersion() {
    return proxyVersion;
  }

  /**
   * Get list of plugins which will be used to reply to the query.
   *
   * @return collection of plugins
   */
  public Collection<PluginInformation> getPlugins() {
    return plugins;
  }


  /**
   * Creates a new {@link Builder} instance from data represented by this response.
   *
   * @return {@link QueryResponse} builder
   */
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
   * Creates a new {@link Builder} instance.
   *
   * @return {@link QueryResponse} builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link QueryResponse} objects.
   */
  public static final class Builder {

    @MonotonicNonNull
    private String hostname;

    @MonotonicNonNull
    private String gameVersion;

    @MonotonicNonNull
    private String map;

    @MonotonicNonNull
    private String proxyHost;

    @MonotonicNonNull
    private String proxyVersion;

    private int currentPlayers;
    private int maxPlayers;
    private int proxyPort;

    private List<String> players = new ArrayList<>();
    private List<PluginInformation> plugins = new ArrayList<>();

    private Builder() {
    }

    public Builder hostname(String hostname) {
      this.hostname = Preconditions.checkNotNull(hostname, "hostname");
      return this;
    }

    public Builder gameVersion(String gameVersion) {
      this.gameVersion = Preconditions.checkNotNull(gameVersion, "gameVersion");
      return this;
    }

    public Builder map(String map) {
      this.map = Preconditions.checkNotNull(map, "map");
      return this;
    }

    public Builder currentPlayers(int currentPlayers) {
      Preconditions.checkArgument(currentPlayers >= 0, "currentPlayers cannot be negative");
      this.currentPlayers = currentPlayers;
      return this;
    }

    public Builder maxPlayers(int maxPlayers) {
      Preconditions.checkArgument(maxPlayers >= 0, "maxPlayers cannot be negative");
      this.maxPlayers = maxPlayers;
      return this;
    }

    public Builder proxyHost(String proxyHost) {
      this.proxyHost = Preconditions.checkNotNull(proxyHost, "proxyHost");
      return this;
    }

    /**
     * Sets the port where this proxy is running.
     * @param proxyPort the port where the proxy is running
     * @return this instance, for chaining
     */
    public Builder proxyPort(int proxyPort) {
      Preconditions
          .checkArgument(proxyPort >= 1 && proxyPort <= 65535, "proxyPort must be between 1-65535");
      this.proxyPort = proxyPort;
      return this;
    }

    public Builder players(Collection<String> players) {
      this.players.addAll(Preconditions.checkNotNull(players, "players"));
      return this;
    }

    public Builder players(String... players) {
      this.players.addAll(Arrays.asList(Preconditions.checkNotNull(players, "players")));
      return this;
    }

    public Builder clearPlayers() {
      this.players.clear();
      return this;
    }

    public Builder proxyVersion(String proxyVersion) {
      this.proxyVersion = Preconditions.checkNotNull(proxyVersion, "proxyVersion");
      return this;
    }

    public Builder plugins(Collection<PluginInformation> plugins) {
      this.plugins.addAll(Preconditions.checkNotNull(plugins, "plugins"));
      return this;
    }

    public Builder plugins(PluginInformation... plugins) {
      this.plugins.addAll(Arrays.asList(Preconditions.checkNotNull(plugins, "plugins")));
      return this;
    }

    /**
     * Clears all currently set plugins.
     *
     * @return this builder, for chaining
     */
    public Builder clearPlugins() {
      this.plugins.clear();
      return this;
    }

    /**
     * Builds a new {@link QueryResponse} with the supplied data. The current instance can be reused
     * after this method is called.
     *
     * @return response
     */
    public QueryResponse build() {
      return new QueryResponse(
          Preconditions.checkNotNull(hostname, "hostname"),
          Preconditions.checkNotNull(gameVersion, "gameVersion"),
          Preconditions.checkNotNull(map, "map"),
          currentPlayers,
          maxPlayers,
          Preconditions.checkNotNull(proxyHost, "proxyHost"),
          proxyPort,
          ImmutableList.copyOf(players),
          Preconditions.checkNotNull(proxyVersion, "proxyVersion"),
          ImmutableList.copyOf(plugins)
      );
    }
  }

  /**
   * Represents a plugin in the query response.
   */
  public static class PluginInformation {

    private final String name;
    private final @Nullable String version;

    PluginInformation(String name, @Nullable String version) {
      this.name = Preconditions.checkNotNull(name, "name");
      this.version = version;
    }

    public String getName() {
      return name;
    }

    public Optional<String> getVersion() {
      return Optional.ofNullable(version);
    }

    public static PluginInformation of(String name, @Nullable String version) {
      return new PluginInformation(name, version);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("name", name)
          .add("version", version)
          .toString();
    }
  }
}
