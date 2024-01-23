/*
 * Copyright (C) 2018-2023 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.config;

/**
 * Supported player info forwarding methods.
 */
public enum PlayerInfoForwarding {
  NONE,
  LEGACY,
  BUNGEEGUARD,
  MODERN
}
