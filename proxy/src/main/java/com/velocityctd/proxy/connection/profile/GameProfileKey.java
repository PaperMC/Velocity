package com.velocityctd.proxy.connection.profile;

import java.util.Objects;

public class GameProfileKey {

    private final String username;
    private final String ip;

    public GameProfileKey(final String username, final String ip) {
        this.username = username;
        this.ip = ip;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GameProfileKey that = (GameProfileKey) o;
        return Objects.equals(username, that.username) && Objects.equals(ip, that.ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, ip);
    }
}
