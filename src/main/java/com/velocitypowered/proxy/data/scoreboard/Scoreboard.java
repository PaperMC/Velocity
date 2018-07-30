package com.velocitypowered.proxy.data.scoreboard;

import java.util.ArrayList;
import java.util.List;

public class Scoreboard {
    private String name;
    private byte position;
    private final List<Objective> objectives = new ArrayList<>();

    public Scoreboard() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte getPosition() {
        return position;
    }

    public void setPosition(byte position) {
        this.position = position;
    }

    public List<Objective> getObjectives() {
        return objectives;
    }

    @Override
    public String toString() {
        return "Scoreboard{" +
                "name='" + name + '\'' +
                ", position=" + position +
                ", objectives=" + objectives +
                '}';
    }
}
