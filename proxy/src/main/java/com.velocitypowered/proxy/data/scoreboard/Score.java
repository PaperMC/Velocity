package com.velocitypowered.proxy.data.scoreboard;

import java.util.Objects;

public class Score {
    private final String target;
    private final int value;

    public Score(String target, int value) {
        this.target = target;
        this.value = value;
    }

    public String getTarget() {
        return target;
    }

    public int getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Score score = (Score) o;
        return value == score.value &&
                Objects.equals(target, score.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(target, value);
    }

    @Override
    public String toString() {
        return "Score{" +
                "target='" + target + '\'' +
                ", value=" + value +
                '}';
    }
}
