package com.velocitypowered.api.proxy.server;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.config.PlayerInfoForwarding;
import java.net.InetSocketAddress;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ServerInfo represents a server that a player can connect to. This object is immutable and safe
 * for concurrent access.
 */
public final class ServerInfo implements Comparable<ServerInfo> {

  private final String name;
  private final InetSocketAddress address;
  private final PlayerInfoForwarding playerInfoForwarding;

  /**
   * Creates a new ServerInfo object.
   *
   * @param name the name for the server
   * @param address the address of the server to connect to
   * @deprecated prefer using constructor where you can specify player info forwarding mode
   */
  @Deprecated
  public ServerInfo(String name, InetSocketAddress address) {
    this(name, address, PlayerInfoForwarding.NONE); // TODO: get default
  }

  /**
   * Creates a new ServerInfo object.
   *
   * @param name the name for the server
   * @param address the address of the server to connect to
   * @param playerInfoForwarding the player info forwarding mode this connection will use
   */
  public ServerInfo(String name, InetSocketAddress address,
                    PlayerInfoForwarding playerInfoForwarding) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.address = Preconditions.checkNotNull(address, "address");
    this.playerInfoForwarding = Preconditions.checkNotNull(playerInfoForwarding,
        "playerInfoForwarding");
  }

  public final String getName() {
    return name;
  }

  public final InetSocketAddress getAddress() {
    return address;
  }

  public final PlayerInfoForwarding getPlayerInfoForwarding() {
    return playerInfoForwarding;
  }

  @Override
  public String toString() {
    return "ServerInfo{"
        + "name='" + name + '\''
        + ", address=" + address
        + ", playerInfoForwarding=" + playerInfoForwarding
        + '}';
  }

  @Override
  public final boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ServerInfo that = (ServerInfo) o;
    return Objects.equals(name, that.name)
        && Objects.equals(address, that.address)
        && Objects.equals(playerInfoForwarding, that.playerInfoForwarding);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(name, address, playerInfoForwarding);
  }

  @Override
  public int compareTo(ServerInfo o) {
    return this.name.compareTo(o.getName());
  }
}
