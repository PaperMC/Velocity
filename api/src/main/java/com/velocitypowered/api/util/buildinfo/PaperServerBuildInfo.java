/*
 * Copyright (C) 2024 Velocity Contributors
 *
 * The Velocity API is licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in the api top-level directory.
 */

package com.velocitypowered.api.util.buildinfo;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Information about the current server build.
 *
 * @apiNote to be seperated later
 */
@SuppressWarnings({"checkstyle", "CheckStyle"}) // Temporarily
@ApiStatus.NonExtendable
public interface PaperServerBuildInfo extends ServerBuildInfo {

    /**
     * Gets the Minecraft version id.
     *
     * @return the Minecraft version id (e.g. "1.20.4", "1.20.2-pre2", "23w31a")
     */
    @NotNull String minecraftVersionId();

    /**
     * Gets the Minecraft version name.
     *
     * @return the Minecraft version name (e.g. "1.20.4", "1.20.2 Pre-release 2", "23w31a")
     */
    @NotNull String minecraftVersionName();
}
