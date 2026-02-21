/*
 * Copyright (C) 2018-2025 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

/**
 * Supported per-server player info forwarding methods.
 *
 * @since 3.4.0
 */
public enum ServerInfoForwardingMode {
    MODERN,
    BUNGEEGUARD,
    LEGACY,
    NONE
}

