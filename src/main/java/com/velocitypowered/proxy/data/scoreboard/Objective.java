package com.velocitypowered.proxy.data.scoreboard;

import net.kyori.text.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Objective {
    private final String id;
    private Component displayName;
    private ObjectiveMode type;
    private final List<Team> teams = new ArrayList<>();
    private final Map<String, Score> scores = new HashMap<>();

    public Objective(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public void setDisplayName(Component displayName) {
        this.displayName = displayName;
    }

    public ObjectiveMode getType() {
        return type;
    }

    public void setType(ObjectiveMode type) {
        this.type = type;
    }

    public List<Team> getTeams() {
        return teams;
    }

    public Map<String, Score> getScores() {
        return scores;
    }

    @Override
    public String toString() {
        return "Objective{" +
                "id='" + id + '\'' +
                ", displayName='" + displayName + '\'' +
                ", type='" + type + '\'' +
                ", teams=" + teams +
                ", scores=" + scores +
                '}';
    }
}
