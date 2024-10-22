/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util.buildinfo;

import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Information about the current server build.
 *
 * @apiNote to be separated later
 */
@SuppressWarnings({"checkstyle", "CheckStyle"}) // Temporarily
@ApiStatus.NonExtendable
public interface VelocityServerBuildInfo extends ServerBuildInfo {

    /**
     * The brand id for Velocity.
     */
    Key BRAND_VELOCITY_ID = Key.key("papermc", "velocity");

    /**
     * Gets the Velocity version id.
     *
     * @return the Velocity version id (e.g. "3.3.0-SNAPSHOT", "3.3.0", "3.0.0")
     */
    @NotNull
    String velocityVersionId();
    // one of these can probably go
    /**
     * Gets the Velocity version name.
     *
     * @return the Velocity version name (e.g. "3.3.0 Snapshot", "3.3.0", "3.0.0")
     */
    @NotNull
    String velocityVersionName();
}
