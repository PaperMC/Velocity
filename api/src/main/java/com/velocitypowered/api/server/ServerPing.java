package com.velocitypowered.api.server;

import net.kyori.text.Component;

/**
 * Represents a 1.7 and above server list ping response. This class is immutable.
 */
public class ServerPing {
    private final Version version;
    private final Players players;
    private final Component description;
    private final Favicon favicon;

    public ServerPing(Version version, Players players, Component description, Favicon favicon) {
        this.version = version;
        this.players = players;
        this.description = description;
        this.favicon = favicon;
    }

    public Version getVersion() {
        return version;
    }

    public Players getPlayers() {
        return players;
    }

    public Component getDescription() {
        return description;
    }

    public Favicon getFavicon() {
        return favicon;
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

    public static class Version {
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

    public static class Players {
        private final int online;
        private final int max;

        public Players(int online, int max) {
            this.online = online;
            this.max = max;
        }

        public int getOnline() {
            return online;
        }

        public int getMax() {
            return max;
        }

        @Override
        public String toString() {
            return "Players{" +
                    "online=" + online +
                    ", max=" + max +
                    '}';
        }
    }
}
