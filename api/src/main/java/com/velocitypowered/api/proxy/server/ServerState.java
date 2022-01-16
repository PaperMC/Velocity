/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.messages.ChannelMessageSink;
import java.util.Collection;
import net.kyori.adventure.audience.Audience;

/**
 * A server's current state. This differs from {@link RegisteredServer} for providing a stateful view
 * on a backend server instance.
 *
 * <p>The {@code Audience} associated with a {@code ServerState} represent all players on the server
 * connected to this proxy and do not interact with the server in any way.
 * </p>
 */
public interface ServerState extends ChannelMessageSink, Audience {
  /**
   * Returns an <strong>immutable</strong> list of all the players currently connected to this server on this proxy.
   *
   * @return the players on the proxy connected to this server
   */
  Collection<Player> getPlayersConnected();
}
