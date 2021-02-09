package com.velocitypowered.api.proxy.server;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import net.kyori.adventure.text.serializer.legacytext3.LegacyText3ComponentSerializer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a 1.7 and above server list ping response. This class is immutable.
 */
public final class ServerPing {

  private final Version version;
  private final @Nullable Players players;
  private final net.kyori.adventure.text.Component description;
  private final @Nullable Favicon favicon;
  private final @Nullable ModInfo modinfo;

  @Deprecated
  public ServerPing(Version version, @Nullable Players players,
      net.kyori.text.Component description, @Nullable Favicon favicon) {
    this(version, players, LegacyText3ComponentSerializer.get().deserialize(description), favicon,
        ModInfo.DEFAULT);
  }

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
  @Deprecated
  public ServerPing(Version version, @Nullable Players players,
      net.kyori.text.Component description, @Nullable Favicon favicon,
      @Nullable ModInfo modinfo) {
    this(version, players, LegacyText3ComponentSerializer.get().deserialize(description), favicon,
        modinfo);
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
      net.kyori.adventure.text.Component description, @Nullable Favicon favicon,
      @Nullable ModInfo modinfo) {
    this.version = Preconditions.checkNotNull(version, "version");
    this.players = players;
    this.description = Preconditions.checkNotNull(description, "description");
    this.favicon = favicon;
    this.modinfo = modinfo;
  }

  public Version getVersion() {
    return version;
  }

  public Optional<Players> getPlayers() {
    return Optional.ofNullable(players);
  }

  @Deprecated
  public net.kyori.text.Component getDescription() {
    return LegacyText3ComponentSerializer.get().serialize(description);
  }

  public net.kyori.adventure.text.Component getDescriptionComponent() {
    return description;
  }

  public Optional<Favicon> getFavicon() {
    return Optional.ofNullable(favicon);
  }

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
    private net.kyori.adventure.text.Component description;
    private @Nullable Favicon favicon;
    private boolean nullOutPlayers;
    private boolean nullOutModinfo;

    private Builder() {

    }

    public Builder version(Version version) {
      this.version = Preconditions.checkNotNull(version, "version");
      return this;
    }

    public Builder onlinePlayers(int onlinePlayers) {
      this.onlinePlayers = onlinePlayers;
      return this;
    }

    public Builder maximumPlayers(int maximumPlayers) {
      this.maximumPlayers = maximumPlayers;
      return this;
    }

    public Builder samplePlayers(SamplePlayer... players) {
      this.samplePlayers.addAll(Arrays.asList(players));
      return this;
    }

    public Builder modType(String modType) {
      this.modType = Preconditions.checkNotNull(modType, "modType");
      return this;
    }

    public Builder mods(ModInfo.Mod... mods) {
      this.mods.addAll(Arrays.asList(mods));
      return this;
    }

    /**
     * Uses the modified {@code mods} list in the response.
     * @param mods the mods list to use
     * @return this build, for chaining
     */
    public Builder mods(ModInfo mods) {
      Preconditions.checkNotNull(mods, "mods");
      this.modType = mods.getType();
      this.mods.clear();
      this.mods.addAll(mods.getMods());
      return this;
    }

    public Builder clearMods() {
      this.mods.clear();
      return this;
    }

    public Builder clearSamplePlayers() {
      this.samplePlayers.clear();
      return this;
    }

    public Builder notModCompatible() {
      this.nullOutModinfo = true;
      return this;
    }

    public Builder nullPlayers() {
      this.nullOutPlayers = true;
      return this;
    }

    @Deprecated
    public Builder description(net.kyori.text.Component description) {
      this.description(LegacyText3ComponentSerializer.get().deserialize(description));
      return this;
    }

    public Builder description(net.kyori.adventure.text.Component description) {
      this.description = Preconditions.checkNotNull(description, "description");
      return this;
    }

    public Builder favicon(Favicon favicon) {
      this.favicon = Preconditions.checkNotNull(favicon, "favicon");
      return this;
    }

    public Builder clearFavicon() {
      this.favicon = null;
      return this;
    }

    /**
     * Uses the information from this builder to create a new {@link ServerPing} instance. The
     * builder can be re-used after this event has been called.
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

    public Version getVersion() {
      return version;
    }

    public int getOnlinePlayers() {
      return onlinePlayers;
    }

    public int getMaximumPlayers() {
      return maximumPlayers;
    }

    public List<SamplePlayer> getSamplePlayers() {
      return samplePlayers;
    }

    @Deprecated
    public Optional<net.kyori.text.Component> getDescription() {
      return Optional.ofNullable(description).map(LegacyText3ComponentSerializer.get()::serialize);
    }

    public Optional<net.kyori.adventure.text.Component> getDescriptionComponent() {
      return Optional.ofNullable(description);
    }

    public Optional<Favicon> getFavicon() {
      return Optional.ofNullable(favicon);
    }

    public String getModType() {
      return modType;
    }

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

  public static final class Version {

    private final int protocol;
    private final String name;

    /**
     * Creates a new instance.
     * @param protocol the protocol version as an integer
     * @param name a friendly name for the protocol version
     */
    public Version(int protocol, String name) {
      this.protocol = protocol;
      this.name = Preconditions.checkNotNull(name, "name");
    }

    public int getProtocol() {
      return protocol;
    }

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

  public static final class Players {

    private final int online;
    private final int max;
    private final List<SamplePlayer> sample;

    /**
     * Creates a new instance.
     * @param online the number of online players
     * @param max the maximum number of players
     * @param sample a sample of players on the server
     */
    public Players(int online, int max, List<SamplePlayer> sample) {
      this.online = online;
      this.max = max;
      this.sample = ImmutableList.copyOf(sample);
    }

    public int getOnline() {
      return online;
    }

    public int getMax() {
      return max;
    }

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

  public static final class SamplePlayer {

    private final String name;
    private final UUID id;

    public SamplePlayer(String name, UUID id) {
      this.name = name;
      this.id = id;
    }

    public String getName() {
      return name;
    }

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
