/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.ResultedEvent.ComponentResult;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import java.net.InetAddress;
import java.net.SocketAddress;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This event is fired when a handshake is established between a client and the proxy.
 */
public interface ConnectionHandshakeEvent extends ResultedEvent<ComponentResult> {

  InboundConnection connection();

  String currentHostname();

  String originalHostname();

  void setCurrentHostname(String hostname);

  @Nullable SocketAddress currentRemoteHostAddress();

  void setCurrentRemoteHostAddress(@Nullable SocketAddress address);
}
