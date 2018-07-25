package com.velocitypowered.proxy.data;

import net.kyori.text.Component;

public class ServerPing {
    private final Version version;
    private final Players players;
    private final Component description;
    private final String favicon;

    public ServerPing(Version version, Players players, Component description, String favicon) {
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

    public String getFavicon() {
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
