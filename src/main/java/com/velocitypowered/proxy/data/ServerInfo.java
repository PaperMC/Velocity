package com.velocitypowered.proxy.data;

import com.google.common.base.Preconditions;

import java.net.InetSocketAddress;
import java.util.Objects;

public final class ServerInfo {
    private final String name;
    private final InetSocketAddress address;

    public ServerInfo(String name, InetSocketAddress address) {
        this.name = Preconditions.checkNotNull(name, "name");
        this.address = Preconditions.checkNotNull(address, "address");
    }

    public final String getName() {
        return name;
    }

    public final InetSocketAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "ServerInfo{" +
                "name='" + name + '\'' +
                ", address=" + address +
                '}';
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerInfo that = (ServerInfo) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(address, that.address);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(name, address);
    }
}
