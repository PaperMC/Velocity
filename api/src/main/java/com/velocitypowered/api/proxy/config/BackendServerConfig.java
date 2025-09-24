/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.config;

import static java.util.Objects.requireNonNull;

import com.velocitypowered.api.proxy.server.ServerInfoForwardingMode;
import org.jspecify.annotations.NullMarked;

/**
 * Exposes server configuration information that plugins may use.<br>
 *
 * <b>What's the forwarding mode?</b><br>
 * The server can use a different mode to obtain and forward player info.<br>
 * For instance, if you are running a 1.12 (or lower version) server on a velocity proxy with MODERN player info forwarding
 * the server doesn't support MODERN forwarding. So you need to set LEGACY forwarding mode for that server
 * and velocity will use ONLY FOR THAT SERVER the legacy forwarding mode.<br><br>
 *
 * @param address The address of the backend server.
 * @param forwardingMode The forwarding mode of the backend server.
 * @since 3.4.0
 * @see ServerInfoForwardingMode
 * @see com.velocitypowered.api.proxy.server.ServerInfo#ServerInfo(String, java.net.InetSocketAddress, ServerInfoForwardingMode)
 * @apiNote <i><b>TIP:</b> If you need to set this value when creating dynamic servers in your plugins
 *     you can do that by adding the {@link ServerInfoForwardingMode} value as the last parameter
 *     while creating a new {@link com.velocitypowered.api.proxy.server.ServerInfo}.</i>
 */
@NullMarked
public record BackendServerConfig(
        String address,
        ServerInfoForwardingMode forwardingMode
) {
  public BackendServerConfig {
    requireNonNull(address);
    requireNonNull(forwardingMode);
  }

  public BackendServerConfig(final String address) {
    this(address, ServerInfoForwardingMode.FOLLOWUP);
  }
}
