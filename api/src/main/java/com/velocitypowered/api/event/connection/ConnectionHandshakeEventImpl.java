/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.google.common.base.Preconditions;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import java.net.InetAddress;
import java.net.SocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a handshake is established between a client and the proxy.
 */
public final class ConnectionHandshakeEventImpl implements ConnectionHandshakeEvent {

  private final InboundConnection connection;
  private final String originalHostname;
  private String currentHostname;
  private ComponentResult result;
  private SocketAddress currentRemoteAddress;

  public ConnectionHandshakeEventImpl(InboundConnection connection,
      String originalHostname) {
    this.connection = Preconditions.checkNotNull(connection, "connection");
    this.originalHostname = Preconditions.checkNotNull(originalHostname, "originalHostname");
    this.currentHostname = originalHostname;
    this.result = ComponentResult.allowed();
    this.currentRemoteAddress = connection.remoteAddress();
  }

  @Override
  public InboundConnection connection() {
    return connection;
  }

  @Override
  public String currentHostname() {
    return currentHostname;
  }

  @Override
  public String originalHostname() {
    return originalHostname;
  }

  @Override
  public void setCurrentHostname(String hostname) {
    currentHostname = Preconditions.checkNotNull(hostname, "hostname");
  }

  @Override
  public @Nullable SocketAddress currentRemoteHostAddress() {
    return currentRemoteAddress;
  }

  @Override
  public void setCurrentRemoteHostAddress(SocketAddress address) {
    currentRemoteAddress = address;
  }

  @Override
  public ComponentResult result() {
    return result;
  }

  @Override
  public void setResult(ComponentResult result) {
    this.result = Preconditions.checkNotNull(result, "result");
  }

  @Override
  public String toString() {
    return "ConnectionHandshakeEventImpl{"
        + "connection=" + connection
        + ", originalHostname='" + originalHostname + '\''
        + ", currentHostname='" + currentHostname + '\''
        + ", result=" + result
        + ", currentRemoteAddress=" + currentRemoteAddress
        + '}';
  }
}
