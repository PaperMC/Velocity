package com.velocitypowered.proxy.data.scoreboard;

import java.util.Collection;
import java.util.HashSet;

public class Team {
    private final String id;
    private String prefix;
    private String suffix;
    private byte flags;
    private String nameTagVisibility;
    private String collision;
    private byte color;
    private final Collection<String> players = new HashSet<>();

    public Team(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public byte getFlags() {
        return flags;
    }

    public void setFlags(byte flags) {
        this.flags = flags;
    }

    public String getNameTagVisibility() {
        return nameTagVisibility;
    }

    public void setNameTagVisibility(String nameTagVisibility) {
        this.nameTagVisibility = nameTagVisibility;
    }

    public String getCollision() {
        return collision;
    }

    public void setCollision(String collision) {
        this.collision = collision;
    }

    public byte getColor() {
        return color;
    }

    public void setColor(byte color) {
        this.color = color;
    }

    public Collection<String> getPlayers() {
        return players;
    }

    @Override
    public String toString() {
        return "Team{" +
                "id='" + id + '\'' +
                ", prefix='" + prefix + '\'' +
                ", suffix='" + suffix + '\'' +
                ", flags=" + flags +
                ", nameTagVisibility='" + nameTagVisibility + '\'' +
                ", collision='" + collision + '\'' +
                ", color=" + color +
                ", players=" + players +
                '}';
    }
}
