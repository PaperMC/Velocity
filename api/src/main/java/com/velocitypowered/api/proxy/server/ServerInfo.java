/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.google.common.base.Preconditions;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * ServerInfo represents a server that a player can connect to. This object is immutable and safe
 * for concurrent access.
 */
public final class ServerInfo implements Comparable<ServerInfo> {

  private final String name;
  private final SocketAddress address;

  /**
   * Creates a new ServerInfo object.
   *
   * @param name the name for the server
   * @param address the address of the server to connect to
   */
  public ServerInfo(String name, InetSocketAddress address) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.address = Preconditions.checkNotNull(address, "address");
  }

  /**
   * Creates a new ServerInfo object.
   *
   * @param name the name for the server
   * @param address the address of the server to connect to
   */
  public ServerInfo(String name, SocketAddress address) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.address = Preconditions.checkNotNull(address, "address");
  }

  public final String getName() {
    return name;
  }

  /**
   * Returns the address of this server as an {@link InetSocketAddress}.
   *
   * @return the address
   * @deprecated Use {@link #getSocketAddress()} instead, which supports both TCP and Unix socket
   *     addresses.
   */
  @Deprecated
  public final InetSocketAddress getAddress() {
    if (address instanceof InetSocketAddress inet) {
      return inet;
    }
    throw new UnsupportedOperationException(
        "This server is not configured with an InetSocketAddress. Use getSocketAddress() instead.");
  }

  /**
   * Returns the address of this server as a {@link SocketAddress}. This may be an
   * {@link InetSocketAddress} for TCP connections or a Unix domain socket address.
   *
   * @return the address
   */
  public final SocketAddress getSocketAddress() {
    return address;
  }

  @Override
  public String toString() {
    return "ServerInfo{"
        + "name='" + name + '\''
        + ", address=" + address
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
        && Objects.equals(address, that.address);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(name, address);
  }

  @Override
  public int compareTo(ServerInfo o) {
    return this.name.compareTo(o.getName());
  }
}
