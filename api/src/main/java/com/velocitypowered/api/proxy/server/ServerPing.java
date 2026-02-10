/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.Nullable;

/**
 * Represents a 1.7 and above server list ping response. This class is immutable.
 */
public final class ServerPing {

  private final Version version;
  private final @Nullable Players players;
  private final @Nullable Component description;
  private final @Nullable Favicon favicon;
  private final @Nullable ModInfo modinfo;

  /**
   * Constructs an initial ServerPing instance.
   *
   * @param version the version of the server
   * @param players the players on the server, or {@code null} if not shown
   * @param description the MOTD for the server
   * @param favicon the server's favicon, or {@code null} if not set
   */
  public ServerPing(Version version, @Nullable Players players,
      net.kyori.adventure.text.Component description, @Nullable Favicon favicon) {
    this(version, players, description, favicon, ModInfo.DEFAULT);
  }

  /**
   * Constructs a ServerPing instance.
   *
   * @param version the version of the server
   * @param players the players on the server
   * @param description the MOTD for the server
   * @param favicon the server's favicon
   * @param modinfo the mods this server runs
   */
  public ServerPing(Version version, @Nullable Players players,
                    Component description, @Nullable Favicon favicon,
                    @Nullable ModInfo modinfo) {
    this.version = Preconditions.checkNotNull(version, "version");
    this.players = players;
    this.description = Preconditions.checkNotNull(description, "description");
    this.favicon = favicon;
    this.modinfo = modinfo;
  }

  /**
   * Gets the version shown to the client during the ping.
   *
   * @return the version
   */
  public Version getVersion() {
    return version;
  }

  /**
   * Gets the player information shown to the client.
   *
   * @return the player information, or empty if not shown
   */
  public Optional<Players> getPlayers() {
    return Optional.ofNullable(players);
  }

  /**
   * Gets the description (MOTD) component shown in the ping response.
   *
   * @return the description component
   */
  @Nullable
  public Component getDescriptionComponent() {
    return description;
  }

  /**
   * Gets the favicon sent to the client.
   *
   * @return the favicon, or empty if not present
   */
  public Optional<Favicon> getFavicon() {
    return Optional.ofNullable(favicon);
  }

  /**
   * Gets the mod info sent to the client.
   *
   * @return the mod info, or empty if not present
   */
  public Optional<ModInfo> getModinfo() {
    return Optional.ofNullable(modinfo);
  }

  @Override
  public String toString() {
    return "ServerPing{"
        + "version=" + version
        + ", players=" + players
        + ", description=" + description
        + ", favicon=" + favicon
        + ", modinfo=" + modinfo
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerPing ping = (ServerPing) o;
    return Objects.equals(version, ping.version)
        && Objects.equals(players, ping.players)
        && Objects.equals(description, ping.description)
        && Objects.equals(favicon, ping.favicon)
        && Objects.equals(modinfo, ping.modinfo);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, players, description, favicon, modinfo);
  }

  /**
   * Returns a copy of this {@link ServerPing} instance as a builder so that it can be modified.
   * It is guaranteed that {@code ping.asBuilder().build().equals(ping)} is true: that is, if no
   * other changes are made to the returned builder, the built instance will equal the original
   * instance.
   *
   * @return a copy of this instance as a {@link Builder}
   */
  public Builder asBuilder() {
    Builder builder = new Builder();
    builder.version = version;
    if (players != null) {
      builder.onlinePlayers = players.online;
      builder.maximumPlayers = players.max;
      builder.samplePlayers.addAll(players.getSample());
    } else {
      builder.nullOutPlayers = true;
    }
    builder.description = description;
    builder.favicon = favicon;
    builder.nullOutModinfo = modinfo == null;
    if (modinfo != null) {
      builder.modType = modinfo.getType();
      builder.mods.addAll(modinfo.getMods());
    }
    return builder;
  }

  /**
   * Creates a new {@link Builder} for constructing a {@link ServerPing}.
   *
   * @return a new ServerPing builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for {@link ServerPing} objects.
   */
  public static final class Builder {

    private Version version = new Version(0, "Unknown");
    private int onlinePlayers;
    private int maximumPlayers;
    private final List<SamplePlayer> samplePlayers = new ArrayList<>();
    private String modType = "FML";
    private final List<ModInfo.Mod> mods = new ArrayList<>();
    private Component description;
    private @Nullable Favicon favicon;
    private boolean nullOutPlayers;
    private boolean nullOutModinfo;

    private Builder() {

    }

    /**
     * Uses the modified {@code version} info in the response.
     *
     * @param version version info to set
     * @return this builder, for chaining
     */
    public Builder version(Version version) {
      this.version = Preconditions.checkNotNull(version, "version");
      return this;
    }

    /**
     * Uses the modified {@code onlinePlayers} number in the response.
     *
     * @param onlinePlayers number for online players to set
     * @return this builder, for chaining
     */
    public Builder onlinePlayers(int onlinePlayers) {
      this.onlinePlayers = onlinePlayers;
      return this;
    }

    /**
     * Uses the modified {@code maximumPlayers} number in the response.
     * <b>This will not modify the actual maximum players that can join the server.</b>
     *
     * @param maximumPlayers number for maximum players to set
     * @return this builder, for chaining
     */
    public Builder maximumPlayers(int maximumPlayers) {
      this.maximumPlayers = maximumPlayers;
      return this;
    }

    /**
     * Uses the modified {@code players} array in the response.
     *
     * @param players array of SamplePlayers to add
     * @return this builder, for chaining
     */
    public Builder samplePlayers(SamplePlayer... players) {
      this.samplePlayers.addAll(Arrays.asList(players));
      return this;
    }

    /**
     * Uses the modified {@code players} collection in the response.
     *
     * @param players collection of SamplePlayers to add
     * @return this builder, for chaining
     */
    public Builder samplePlayers(Collection<SamplePlayer> players) {
      this.samplePlayers.addAll(players);
      return this;
    }

    /**
     * Uses the modified {@code modType} in the response.
     *
     * @param modType the mod type to set
     * @return this builder, for chaining
     */
    public Builder modType(String modType) {
      this.modType = Preconditions.checkNotNull(modType, "modType");
      return this;
    }

    /**
     * Uses the modified {@code mods} array in the response.
     *
     * @param mods array of mods to use
     * @return this builder, for chaining
     */
    public Builder mods(ModInfo.Mod... mods) {
      this.mods.addAll(Arrays.asList(mods));
      return this;
    }

    /**
     * Uses the modified {@code mods} list in the response.
     *
     * @param mods the mods list to use
     * @return this builder, for chaining
     */
    public Builder mods(ModInfo mods) {
      Preconditions.checkNotNull(mods, "mods");
      this.modType = mods.getType();
      this.mods.clear();
      this.mods.addAll(mods.getMods());
      return this;
    }

    /**
     * Clears the current list of mods to use in the response.
     *
     * @return this builder, for chaining
     */
    public Builder clearMods() {
      this.mods.clear();
      return this;
    }

    /**
     * Clears the current list of PlayerSamples to use in the response.
     *
     * @return this builder, for chaining
     */
    public Builder clearSamplePlayers() {
      this.samplePlayers.clear();
      return this;
    }

    /**
     * Defines the server as mod incompatible in the response.
     *
     * @return this builder, for chaining
     */
    public Builder notModCompatible() {
      this.nullOutModinfo = true;
      return this;
    }

    /**
     * Enables nulling Players in the response.
     * This will display the player count as {@code ???}.
     *
     * @return this builder, for chaining
     */
    public Builder nullPlayers() {
      this.nullOutPlayers = true;
      return this;
    }

    /**
     * Uses the {@code description} Component in the response.
     *
     * @param description Component to use as the description.
     * @return this builder, for chaining
     */
    public Builder description(Component description) {
      this.description = Preconditions.checkNotNull(description, "description");
      return this;
    }

    /**
     * Uses the {@code favicon} in the response.
     *
     * @param favicon Favicon instance to use.
     * @return this builder, for chaining
     */
    public Builder favicon(Favicon favicon) {
      this.favicon = Preconditions.checkNotNull(favicon, "favicon");
      return this;
    }

    /**
     * Clears the current favicon used in the response.
     *
     * @return this builder, for chaining
     */
    public Builder clearFavicon() {
      this.favicon = null;
      return this;
    }

    /**
     * Uses the information from this builder to create a new {@link ServerPing} instance. The
     * builder can be re-used after this event has been called.
     *
     * @return a new {@link ServerPing} instance
     */
    public ServerPing build() {
      if (this.version == null) {
        throw new IllegalStateException("version not specified");
      }
      if (this.description == null) {
        throw new IllegalStateException("no server description supplied");
      }
      return new ServerPing(version,
          nullOutPlayers ? null : new Players(onlinePlayers, maximumPlayers, samplePlayers),
          description, favicon, nullOutModinfo ? null : new ModInfo(modType, mods));
    }

    /**
     * Gets the version currently set in the builder.
     *
     * @return the version
     */
    public Version getVersion() {
      return version;
    }

    /**
     * Gets the number of players online.
     *
     * @return the online player count
     */
    public int getOnlinePlayers() {
      return onlinePlayers;
    }

    /**
     * Gets the maximum player capacity.
     *
     * @return the max player count
     */
    public int getMaximumPlayers() {
      return maximumPlayers;
    }

    /**
     * Gets the sample players shown in the ping.
     *
     * @return the sample player list
     */
    public List<SamplePlayer> getSamplePlayers() {
      return samplePlayers;
    }

    /**
     * Gets the description component currently set in the builder.
     *
     * @return the server description, or empty if unset
     */
    public Optional<Component> getDescriptionComponent() {
      return Optional.ofNullable(description);
    }

    /**
     * Gets the favicon currently set in the builder.
     *
     * @return the favicon, or empty if none
     */
    public Optional<Favicon> getFavicon() {
      return Optional.ofNullable(favicon);
    }

    /**
     * Gets the type of mod loader (e.g., "FML").
     *
     * @return the mod type string
     */
    public String getModType() {
      return modType;
    }

    /**
     * Gets the list of mods reported in the ping.
     *
     * @return the mod list
     */
    public List<ModInfo.Mod> getMods() {
      return mods;
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("version", version)
          .add("onlinePlayers", onlinePlayers)
          .add("maximumPlayers", maximumPlayers)
          .add("samplePlayers", samplePlayers)
          .add("modType", modType)
          .add("mods", mods)
          .add("description", description)
          .add("favicon", favicon)
          .add("nullOutPlayers", nullOutPlayers)
          .add("nullOutModinfo", nullOutModinfo)
          .toString();
    }
  }

  /**
   * Represents the version of the server sent to the client. A protocol version
   * that does not match the client's protocol version will show up on the server
   * list as an incompatible version, but the client will still permit the user
   * to connect to the server anyway.
   */
  public static final class Version {

    private final int protocol;
    private final String name;

    /**
     * Creates a new instance.
     *
     * @param protocol the protocol version as an integer
     * @param name a friendly name for the protocol version
     */
    public Version(int protocol, String name) {
      this.protocol = protocol;
      this.name = Preconditions.checkNotNull(name, "name");
    }

    /**
     * Gets the protocol number associated with the server version.
     *
     * @return the protocol version number
     */
    public int getProtocol() {
      return protocol;
    }

    /**
     * Gets the user-facing name of the server version.
     *
     * @return the version name
     */
    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return "Version{"
          + "protocol=" + protocol
          + ", name='" + name + '\''
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Version version = (Version) o;
      return protocol == version.protocol && Objects.equals(name, version.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(protocol, name);
    }
  }

  /**
   * Represents what the players the server purports to have online, its maximum capacity,
   * and a sample of players on the server.
   */
  public static final class Players {

    private final int online;
    private final int max;
    private final List<SamplePlayer> sample;

    /**
     * Creates a new instance.
     *
     * @param online the number of online players
     * @param max the maximum number of players
     * @param sample a sample of players on the server
     */
    public Players(int online, int max, List<SamplePlayer> sample) {
      this.online = online;
      this.max = max;
      this.sample = ImmutableList.copyOf(sample);
    }

    /**
     * Gets the number of online players.
     *
     * @return the number of online players
     */
    public int getOnline() {
      return online;
    }

    /**
     * Gets the maximum number of players the server claims it can hold.
     *
     * @return the maximum number of players
     */
    public int getMax() {
      return max;
    }

    /**
     * Gets a sample list of online players.
     *
     * @return the sample players
     */
    public List<SamplePlayer> getSample() {
      return sample == null ? ImmutableList.of() : sample;
    }

    @Override
    public String toString() {
      return "Players{"
          + "online=" + online
          + ", max=" + max
          + ", sample=" + sample
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Players players = (Players) o;
      return online == players.online && max == players.max
          && Objects.equals(sample, players.sample);
    }

    @Override
    public int hashCode() {
      return Objects.hash(online, max, sample);
    }
  }

  /**
   * A player returned in the sample field of the server ping players field.
   */
  public static final class SamplePlayer {

    /**
     * A constant representing an anonymous sample player with a null UUID and generic name.
     */
    public static final SamplePlayer ANONYMOUS = new SamplePlayer(
        "Anonymous Player",
        new UUID(0L, 0L)
    );
    /**
     * The legacy string name of the player.
     */
    private final String name;
    /**
     * The unique identifier (UUID) of the player.
     */
    private final UUID id;

    /**
     * Constructs a SamplePlayer with the given name and UUID.
     *
     * @param name the name of the player
     * @param id the UUID of the player
     */
    public SamplePlayer(String name, UUID id) {
      this.name = name;
      this.id = id;
    }

    /**
     * Gets the legacy string name of the sample player.
     *
     * @return the player name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the UUID of the sample player.
     *
     * @return the player UUID
     */
    public UUID getId() {
      return id;
    }

    @Override
    public String toString() {
      return "SamplePlayer{"
          + "name='" + name + '\''
          + ", id=" + id
          + '}';
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      SamplePlayer that = (SamplePlayer) o;
      return Objects.equals(name, that.name) && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, id);
    }
  }
}
