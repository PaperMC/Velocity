/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.google.common.base.Preconditions;
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
  private final ServerInfoForwardingMode forwardingMode;

  /**
   * Creates a new ServerInfo object.
   *
   * @param name the name for the server
   * @param address the address of the server to connect to
   * @param forwardingMode the server info forwarding mode
   * @since 3.4.0
   */
  public ServerInfo(String name, InetSocketAddress address, ServerInfoForwardingMode forwardingMode) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.address = Preconditions.checkNotNull(address, "address");
    this.forwardingMode = Preconditions.checkNotNull(forwardingMode, "forwardingMode");
  }

  /**
   * Creates a new ServerInfo object.
   *
   * @param name the name for the server
   * @param address the address of the server to connect to
   */
  public ServerInfo(String name, InetSocketAddress address) {
    this.name = Preconditions.checkNotNull(name, "name");
    this.address = Preconditions.checkNotNull(address, "address");
    this.forwardingMode = ServerInfoForwardingMode.FOLLOWUP;
  }

  public final String getName() {
    return name;
  }

  public final InetSocketAddress getAddress() {
    return address;
  }

  /**
   * Get what mode will the backend server use to communicate with velocity.
   *
   * @return FOLLOWUP mode if the server uses the same mode as set in the main config else one of the available modes
   */
  public final ServerInfoForwardingMode getServerInfoForwardingMode() {
    return forwardingMode;
  }

  @Override
  public String toString() {
    return "ServerInfo{"
        + "name='" + name + '\''
        + ", address=" + address
        + ", forwarding=" + forwardingMode
        + '}';
  }

  @Override
  public final boolean equals(@Nullable Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof final ServerInfo that)) {
      return false;
    }
    return Objects.equals(name, that.name)
        && Objects.equals(address, that.address)
        && Objects.equals(forwardingMode, that.forwardingMode);
  }

  @Override
  public final int hashCode() {
    return Objects.hash(name, address, forwardingMode);
  }

  @Override
  public int compareTo(ServerInfo o) {
    return this.name.compareTo(o.getName());
  }
}
