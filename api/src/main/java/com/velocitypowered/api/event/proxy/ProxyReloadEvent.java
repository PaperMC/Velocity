/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.event.proxy;

/**
 * This event is fired when the proxy is reloaded by the user using {@code /velocity reload}.
 */
public class ProxyReloadEvent {

  @Override
  public String toString() {
    return "ProxyReloadEvent";
  }
}
