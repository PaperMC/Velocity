/*
 * Copyright (C) 2018-2021 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.proxy.server;

/**
 * Supported server info forwarding methods.
 */
public enum ServerInfoForwardingMode {
    /**
     * This type will follow the value of the player-info-forwarding-mode in the velocity configuration.
     */
    FOLLOWUP,
    MODERN,
    BUNGEEGUARD,
    LEGACY,
    NONE
}

