package com.velocitypowered.api.proxy.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.util.Favicon;
import com.velocitypowered.api.util.ModInfo;
import net.kyori.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

/**
 * Represents a 1.7 and above server list ping response. This class is immutable.
 */
public final class ServerPing {
    private final Version version;
    private final @Nullable Players players;
    private final Component description;
    private final @Nullable Favicon favicon;
    private final @Nullable ModInfo modinfo;

    public ServerPing(Version version, @Nullable Players players, Component description, @Nullable Favicon favicon) {
        this(version, players, description, favicon, ModInfo.DEFAULT);
    }

    public ServerPing(Version version, @Nullable Players players, Component description, @Nullable Favicon favicon, @Nullable ModInfo modinfo) {
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

    public Component getDescription() {
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
        return "ServerPing{" +
                "version=" + version +
                ", players=" + players +
                ", description=" + description +
                ", favicon='" + favicon + '\'' +
                '}';
    }

    public Builder asBuilder() {
        Builder builder = new Builder();
        builder.version = version;
        if (players != null) {
            builder.onlinePlayers = players.online;
            builder.maximumPlayers = players.max;
            builder.samplePlayers.addAll(players.sample);
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
        private @Nullable Component description;
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

        public Builder description(Component description) {
            this.description = Preconditions.checkNotNull(description, "description");
            return this;
        }

        public Builder favicon(Favicon favicon) {
            this.favicon = Preconditions.checkNotNull(favicon, "favicon");
            return this;
        }

        public ServerPing build() {
            if (this.version == null) {
                throw new IllegalStateException("version not specified");
            }
            if (this.description == null) {
                throw new IllegalStateException("no server description supplied");
            }
            return new ServerPing(version, nullOutPlayers ? null : new Players(onlinePlayers, maximumPlayers, samplePlayers),
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

        public Optional<Component> getDescription() {
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
            return "Builder{" +
                    "version=" + version +
                    ", onlinePlayers=" + onlinePlayers +
                    ", maximumPlayers=" + maximumPlayers +
                    ", samplePlayers=" + samplePlayers +
                    ", modType=" + modType +
                    ", mods=" + mods +
                    ", description=" + description +
                    ", favicon=" + favicon +
                    ", nullOutPlayers=" + nullOutPlayers +
                    ", nullOutModinfo=" + nullOutModinfo +
                    '}';
        }
    }

    public static final class Version {
        private final int protocol;
        private final String name;

        public Version(int protocol, String name) {
            this.protocol = protocol;
            this.name = name;
        }

        public int getProtocol() {
            return protocol;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "Version{" +
                    "protocol=" + protocol +
                    ", name='" + name + '\'' +
                    '}';
        }
    }

    public static final class Players {
        private final int online;
        private final int max;
        private final List<SamplePlayer> sample;

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
            return sample;
        }

        @Override
        public String toString() {
            return "Players{" +
                    "online=" + online +
                    ", max=" + max +
                    ", sample=" + sample +
                    '}';
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
            return "SamplePlayer{" +
                    "name='" + name + '\'' +
                    ", id=" + id +
                    '}';
        }
    }
}
