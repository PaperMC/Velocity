/*
 * Copyright (C) 2018 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.network;

import java.net.SocketAddress;

/**
 * Represents a network listener for the proxy.
 */
public interface NetworkEndpoint {
  /**
   * The type.
   *
   * @return the type
   */
  ListenerType type();

  /**
   * The address the listener is listening on.
   *
   * @return the address
   */
  SocketAddress address();
}
