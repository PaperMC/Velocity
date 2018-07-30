package com.velocitypowered.proxy.data.scoreboard;

import java.util.HashMap;
import java.util.Map;

public class Scoreboard {
    private String displayName;
    private byte position;
    private final Map<String, Objective> objectives = new HashMap<>();
    private final Map<String, Team> teams = new HashMap<>();

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public byte getPosition() {
        return position;
    }

    public void setPosition(byte position) {
        this.position = position;
    }

    public Map<String, Objective> getObjectives() {
        return objectives;
    }

    public Map<String, Team> getTeams() {
        return teams;
    }

    @Override
    public String toString() {
        return "Scoreboard{" +
                "displayName='" + displayName + '\'' +
                ", position=" + position +
                ", objectives=" + objectives +
                ", teams=" + teams +
                '}';
    }
}
