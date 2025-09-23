/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.config;

import com.velocitypowered.api.proxy.server.ServerInfoForwardingMode;

/**
 * Exposes server configuration information that plugins may use.
 */
public interface BackendServerConfig {

  /**
   * Get the address of the backend server.
   *
   * @return the address formatted as address:port
   */
  String getAddress();

  /**
   * Get the info forwarding mode of the server<br><br>
   * <b>What's this?</b><br>
   * The server can use a different mode to obtain and forward player info.<br>
   * For instance, if you are running a 1.12 (or lower version) server on a velocity proxy with MODERN player info forwarding
   * the server doesn't support MODERN forwarding. So you need to set LEGACY forwarding mode for that server
   * and velocity will use ONLY FOR THAT SERVER the legacy forwarding mode.<br><br>
   * <i><b>TIP:</b> If you need to set this value when creating dynamic servers in your plugins you can do that by adding
   * the ServerInfoForwardingMode value as the last parameter while creating a new server info.</i>
   *
   * @return the server info forwarding mode
   */

  ServerInfoForwardingMode getForwardingMode();

}
