package com.velocitypowered.api.proxy.server;

import com.google.common.base.Preconditions;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.InetSocketAddress;
import java.util.Objects;

/**
 * ServerInfo represents a server that a player can connect to. This object is immutable and safe for concurrent access.
 */
public final class ServerInfo {
    private final @NonNull String name;
    private final @NonNull InetSocketAddress address;

    /**
     * Creates a new ServerInfo object.
     * @param name the name for the server
     * @param address the address of the server to connect to
     */
    public ServerInfo(@NonNull String name, @NonNull InetSocketAddress address) {
        this.name = Preconditions.checkNotNull(name, "name");
        this.address = Preconditions.checkNotNull(address, "address");
    }

    public final @NonNull String getName() {
        return name;
    }

    public final @NonNull InetSocketAddress getAddress() {
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
