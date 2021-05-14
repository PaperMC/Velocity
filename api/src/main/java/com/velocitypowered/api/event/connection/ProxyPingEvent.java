/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.event.Event;
import com.velocitypowered.api.proxy.connection.InboundConnection;
import com.velocitypowered.api.proxy.server.ServerPing;

/**
 * This event is fired when a server list ping request is sent by a remote client.
 */
public interface ProxyPingEvent extends Event {

  InboundConnection connection();

  ServerPing ping();

  void setPing(ServerPing ping);
}
